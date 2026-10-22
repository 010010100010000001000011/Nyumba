import { getFirestore, Transaction } from "firebase-admin/firestore";
import { HttpsError, onCall, onRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import { randomBytes } from "node:crypto";
import { allocatePayment, outstanding, RentPeriodDoc } from "./allocation";
import {
  getTransactionStatus,
  LIPILA_API_KEY,
  normalizeZambianPhone,
  PaymentStatus,
  requestMobileMoneyCollection,
} from "./lipila";

/** Must match FUNCTIONS_REGION in the Android app's AppModule. */
export const REGION = "africa-south1";

const MAX_AMOUNT = 100_000;
/** Pending intents older than this are marked failed by reconciliation. */
const INTENT_TIMEOUT_MS = 30 * 60 * 1000;

const db = () => getFirestore();

interface PaymentIntent {
  referenceId: string;
  tenantId: string;
  landlordId: string;
  boardingHouseId: string;
  roomId: string;
  amount: number;
  phoneNumber: string;
  notes: string | null;
  status: PaymentStatus;
  message: string | null;
  lipilaIdentifier: string | null;
  paymentId: string | null;
  receiptId: string | null;
  createdAt: number;
  updatedAt: number;
}

function newReferenceId(): string {
  return `NYB${Date.now()}${randomBytes(3).toString("hex").toUpperCase()}`;
}

function kwacha(amount: number): string {
  return `K${amount.toFixed(2)}`;
}

/**
 * Callable from the tenant app: starts a Lipila mobile-money collection for the caller's rent.
 * Request:  { phoneNumber: string, amount: number, notes?: string }
 * Response: { referenceId: string, status: "PENDING" | "SUCCESSFUL" | "FAILED", message?: string }
 */
export const initiateLipilaPayment = onCall(
  { region: REGION, secrets: [LIPILA_API_KEY] },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign in to make a payment.");

    const { phoneNumber, amount, notes } = (request.data ?? {}) as Record<string, unknown>;
    if (typeof amount !== "number" || !Number.isFinite(amount) || amount <= 0 || amount > MAX_AMOUNT) {
      throw new HttpsError("invalid-argument", `Amount must be between K1 and ${kwacha(MAX_AMOUNT)}.`);
    }
    if (Math.round(amount * 100) !== amount * 100) {
      throw new HttpsError("invalid-argument", "Amount can have at most two decimal places.");
    }
    const accountNumber = typeof phoneNumber === "string" ? normalizeZambianPhone(phoneNumber) : null;
    if (!accountNumber) {
      throw new HttpsError("invalid-argument", "Enter a valid Zambian mobile number, e.g. 0977123456.");
    }

    const userSnap = await db().collection("users").doc(uid).get();
    const user = userSnap.data();
    if (!user || user.role !== "TENANT") throw new HttpsError("permission-denied", "Only tenants can pay rent.");
    if (!user.boardingHouseId || !user.currentRoomId) {
      throw new HttpsError("failed-precondition", "You need an assigned room before paying rent.");
    }
    const houseSnap = await db().collection("boardingHouses").doc(user.boardingHouseId).get();
    const landlordId = houseSnap.get("landlordId") as string | undefined;
    if (!landlordId) throw new HttpsError("failed-precondition", "Your boarding house could not be found.");

    const now = Date.now();
    const intent: PaymentIntent = {
      referenceId: newReferenceId(),
      tenantId: uid,
      landlordId,
      boardingHouseId: user.boardingHouseId,
      roomId: user.currentRoomId,
      amount,
      phoneNumber: accountNumber,
      notes: typeof notes === "string" && notes.trim() ? notes.trim().slice(0, 200) : null,
      status: "PENDING",
      message: "Waiting for you to approve the prompt on your phone.",
      lipilaIdentifier: null,
      paymentId: null,
      receiptId: null,
      createdAt: now,
      updatedAt: now,
    };
    const intentRef = db().collection("paymentIntents").doc(intent.referenceId);
    await intentRef.set(intent);

    try {
      const result = await requestMobileMoneyCollection({
        referenceId: intent.referenceId,
        amount,
        accountNumber,
        narration: `Nyumba rent ${houseSnap.get("name") ?? ""}`.trim().slice(0, 60),
        email: typeof user.email === "string" ? user.email : undefined,
      });
      await intentRef.update({ lipilaIdentifier: result.identifier, updatedAt: Date.now() });
      if (result.status !== "PENDING") {
        await applyStatus(intent.referenceId, result.status, result.message);
      }
      const latest = (await intentRef.get()).data() as PaymentIntent;
      return { referenceId: intent.referenceId, status: latest.status, message: latest.message };
    } catch (err) {
      logger.error("Lipila collection request failed", { referenceId: intent.referenceId, err });
      await intentRef.update({
        status: "FAILED",
        message: "The payment could not be started. Please try again.",
        updatedAt: Date.now(),
      });
      throw new HttpsError("unavailable", "The payment could not be started. Please try again.");
    }
  },
);

/**
 * Webhook Lipila calls when a transaction completes. The body is only used to learn the
 * referenceId; the outcome is always re-fetched from Lipila so a forged callback cannot
 * mark a payment as successful.
 */
export const lipilaCallback = onRequest({ region: REGION, secrets: [LIPILA_API_KEY] }, async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }
  const referenceId = (req.body as { referenceId?: unknown } | undefined)?.referenceId;
  if (typeof referenceId !== "string" || !/^[A-Za-z0-9_-]{1,64}$/.test(referenceId)) {
    res.status(400).send("Missing referenceId");
    return;
  }
  const intentSnap = await db().collection("paymentIntents").doc(referenceId).get();
  if (!intentSnap.exists) {
    // Not ours (or already cleaned up); acknowledge so Lipila stops retrying.
    res.status(200).send("ignored");
    return;
  }
  try {
    const verified = await getTransactionStatus(referenceId);
    await applyStatus(referenceId, verified.status, verified.message);
    res.status(200).send("ok");
  } catch (err) {
    logger.error("Callback verification failed", { referenceId, err });
    res.status(500).send("retry"); // let Lipila retry; reconciliation is the backstop
  }
});

/** Backstop for missed callbacks: polls Lipila for pending intents and times out stale ones. */
export const reconcileLipilaPayments = onSchedule(
  { region: REGION, schedule: "every 5 minutes", secrets: [LIPILA_API_KEY] },
  async () => {
    const now = Date.now();
    const pending = await db()
      .collection("paymentIntents")
      .where("status", "==", "PENDING")
      .where("createdAt", "<", now - 60_000)
      .limit(50)
      .get();

    for (const doc of pending.docs) {
      const intent = doc.data() as PaymentIntent;
      try {
        const result = await getTransactionStatus(intent.referenceId);
        if (result.status === "PENDING" && now - intent.createdAt > INTENT_TIMEOUT_MS) {
          await applyStatus(intent.referenceId, "FAILED", "The payment was not approved in time.");
        } else {
          await applyStatus(intent.referenceId, result.status, result.message);
        }
      } catch (err) {
        logger.warn("Reconciliation status check failed", { referenceId: intent.referenceId, err });
      }
    }
  },
);

/** Moves a PENDING intent to its final state. Safe to call repeatedly (idempotent). */
export async function applyStatus(referenceId: string, status: PaymentStatus, message: string | null): Promise<void> {
  if (status === "PENDING") return;
  const intentRef = db().collection("paymentIntents").doc(referenceId);

  await db().runTransaction(async (tx) => {
    const intent = (await tx.get(intentRef)).data() as PaymentIntent | undefined;
    if (!intent || intent.status !== "PENDING") return;

    if (status === "FAILED") {
      tx.update(intentRef, {
        status: "FAILED",
        message: message ?? "The payment was declined or cancelled.",
        updatedAt: Date.now(),
      });
      return;
    }
    await recordSuccessfulPayment(tx, intent);
  });
}

/**
 * Records a confirmed payment exactly once: payment, rent allocation, receipt,
 * notifications and audit log, mirroring RentRepositoryImpl.processPaymentAllocation.
 */
async function recordSuccessfulPayment(tx: Transaction, intent: PaymentIntent): Promise<void> {
  const firestore = db();
  const now = Date.now();

  // All reads before any writes (Firestore transaction requirement)
  const periodsSnap = await tx.get(
    firestore
      .collection("rentPeriods")
      .where("tenantId", "==", intent.tenantId)
      .where("boardingHouseId", "==", intent.boardingHouseId),
  );
  const tenantSnap = await tx.get(firestore.collection("users").doc(intent.tenantId));
  const roomSnap = await tx.get(firestore.collection("rooms").doc(intent.roomId));

  const unpaid: RentPeriodDoc[] = periodsSnap.docs
    .map((d) => ({ ...(d.data() as RentPeriodDoc), periodId: d.id }))
    .filter((p) => p.status !== "PAID");
  const previousBalance = outstanding(unpaid);
  const allocation = allocatePayment(intent.amount, unpaid);

  const paymentRef = firestore.collection("payments").doc();
  const receiptRef = firestore.collection("receipts").doc();

  tx.set(paymentRef, {
    paymentId: paymentRef.id,
    tenantId: intent.tenantId,
    landlordId: intent.landlordId,
    boardingHouseId: intent.boardingHouseId,
    roomId: intent.roomId,
    amount: intent.amount,
    paymentMethod: "MOBILE_MONEY",
    status: "SUCCESSFUL",
    transactionId: intent.lipilaIdentifier ?? intent.referenceId,
    referenceId: intent.referenceId,
    paymentDate: now,
    createdAt: now,
    createdBy: intent.tenantId,
    notes: intent.notes,
  });

  for (const period of allocation.changed) {
    tx.update(firestore.collection("rentPeriods").doc(period.periodId), {
      amountPaid: period.amountPaid,
      status: period.status,
      updatedAt: now,
    });
  }

  const remainingBalance = outstanding(allocation.periods);
  tx.set(receiptRef, {
    receiptId: receiptRef.id,
    receiptNumber: `REC-${Math.floor(now / 1000)}`,
    paymentId: paymentRef.id,
    tenantId: intent.tenantId,
    landlordId: intent.landlordId,
    boardingHouseId: intent.boardingHouseId,
    roomId: intent.roomId,
    tenantName: tenantSnap.get("fullName") ?? "Tenant",
    roomName: roomSnap.get("roomName") ?? "Room",
    amountPaid: intent.amount,
    previousBalance,
    remainingBalance,
    advanceCoverage: 0,
    paymentMethod: "MOBILE_MONEY",
    transactionId: intent.lipilaIdentifier ?? intent.referenceId,
    paymentDate: now,
    generatedAt: now,
  });

  const notify = (fields: { tenantId?: string; landlordId?: string; title: string; message: string }) => {
    const ref = firestore.collection("notifications").doc();
    tx.set(ref, {
      notificationId: ref.id,
      tenantId: fields.tenantId ?? null,
      landlordId: fields.landlordId ?? null,
      boardingHouseId: intent.boardingHouseId,
      title: fields.title,
      message: fields.message,
      category: "PAYMENT",
      isRead: false,
      createdAt: now,
    });
  };
  notify({
    tenantId: intent.tenantId,
    title: "Payment successful",
    message: `Your payment of ${kwacha(intent.amount)} was received. Your receipt is ready.`,
  });
  notify({
    landlordId: intent.landlordId,
    title: "Rent received",
    message: `${tenantSnap.get("fullName") ?? "A tenant"} paid ${kwacha(intent.amount)} via mobile money.`,
  });

  const logRef = firestore.collection("auditLogs").doc();
  tx.set(logRef, {
    logId: logRef.id,
    actorId: intent.tenantId,
    actorRole: "TENANT",
    action: "PAYMENT_RECORDED",
    entityType: "PAYMENT",
    entityId: paymentRef.id,
    boardingHouseId: intent.boardingHouseId,
    timestamp: now,
    metadata: {
      amount: String(intent.amount),
      method: "MOBILE_MONEY",
      referenceId: intent.referenceId,
      ...(allocation.unallocated > 0 ? { unallocated: String(allocation.unallocated) } : {}),
    },
  });

  tx.update(firestore.collection("paymentIntents").doc(intent.referenceId), {
    status: "SUCCESSFUL",
    message: "Payment received.",
    paymentId: paymentRef.id,
    receiptId: receiptRef.id,
    updatedAt: now,
  });
}
