/**
 * Integration tests for server-side payment recording (applyStatus) against the Firestore emulator.
 * Run with:  npm run test:emulator
 */
import { before, beforeEach, describe, test } from "node:test";
import assert from "node:assert/strict";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { currentYearMonth } from "../allocation";

initializeApp({ projectId: "demo-nyumba" });
const db = getFirestore();

// Imported after initializeApp so the module's getFirestore() uses the emulator app
// eslint-disable-next-line @typescript-eslint/no-require-imports
const { applyStatus } = require("../payments") as typeof import("../payments");

const { year, month } = currentYearMonth(Date.now());
const next = month === 12 ? { year: year + 1, month: 1 } : { year, month: month + 1 };
const prev = month === 1 ? { year: year - 1, month: 12 } : { year, month: month - 1 };

async function clear() {
  for (const col of ["users", "boardingHouses", "rooms", "rentPeriods", "payments", "receipts", "notifications", "auditLogs", "paymentIntents"]) {
    const snap = await db.collection(col).get();
    await Promise.all(snap.docs.map((d) => d.ref.delete()));
  }
}

async function seed(amount: number, ref = "REF1") {
  await db.doc("users/T").set({ uid: "T", role: "TENANT", fullName: "Tenant T", boardingHouseId: "H1", currentRoomId: "R1" });
  await db.doc("rooms/R1").set({ roomId: "R1", roomName: "Room 1", boardingHouseId: "H1" });
  const p = (id: string, ym: { year: number; month: number }, house = "H1", paid = 0) =>
    db.doc(`rentPeriods/${id}`).set({
      periodId: id, tenantId: "T", boardingHouseId: house, year: ym.year, month: ym.month,
      amountDue: 1000, amountPaid: paid, status: paid > 0 ? "PARTIALLY_PAID" : "UNPAID",
    });
  await p("PREV", prev, "H1", 400);   // 600 overdue
  await p("CUR", { year, month });    // 1000 due this month
  await p("NEXT", next);              // 1000 next month
  await p("OTHER", prev, "H9");       // a different house: must never be touched
  await db.doc(`paymentIntents/${ref}`).set({
    referenceId: ref, tenantId: "T", landlordId: "A", boardingHouseId: "H1", roomId: "R1",
    amount, phoneNumber: "260977000001", notes: null, status: "PENDING", message: null,
    lipilaIdentifier: "LIP-1", paymentId: null, receiptId: null, createdAt: Date.now(), updatedAt: Date.now(),
  });
}

const count = async (col: string) => (await db.collection(col).get()).size;
const period = async (id: string) => (await db.doc(`rentPeriods/${id}`).get()).data()!;

before(() => {
  assert.ok(process.env.FIRESTORE_EMULATOR_HOST, "run via `npm run test:emulator` so the emulator is used");
});

beforeEach(clear);

describe("applyStatus", () => {
  test("a confirmed payment records payment, allocation, receipt, notifications and audit atomically", async () => {
    await seed(2600); // 600 overdue + 1000 current + 1000 next month
    await applyStatus("REF1", "SUCCESSFUL", "ok");

    const intent = (await db.doc("paymentIntents/REF1").get()).data()!;
    assert.equal(intent.status, "SUCCESSFUL");
    assert.ok(intent.paymentId && intent.receiptId);

    const payment = (await db.doc(`payments/${intent.paymentId}`).get()).data()!;
    assert.equal(payment.amount, 2600);
    assert.equal(payment.paymentMethod, "MOBILE_MONEY");
    assert.equal(payment.landlordId, "A");
    assert.equal(payment.transactionId, "LIP-1");

    assert.deepEqual([(await period("PREV")).status, (await period("CUR")).status, (await period("NEXT")).status], ["PAID", "PAID", "ADVANCE_PAID"]);
    assert.equal((await period("OTHER")).amountPaid, 0, "periods in another house are untouched");

    const receipt = (await db.doc(`receipts/${intent.receiptId}`).get()).data()!;
    assert.equal(receipt.previousBalance, 1600, "due before = 600 overdue + 1000 current (not next month)");
    assert.equal(receipt.remainingBalance, 0);
    assert.equal(receipt.advanceCoverage, 1);
    assert.equal(receipt.tenantName, "Tenant T");
    assert.equal(receipt.roomName, "Room 1");

    assert.equal(await count("notifications"), 2, "tenant + landlord are notified");
    assert.equal(await count("auditLogs"), 1);
  });

  test("a duplicate callback does not record the payment twice", async () => {
    await seed(1000);
    await applyStatus("REF1", "SUCCESSFUL", null);
    await applyStatus("REF1", "SUCCESSFUL", null);
    assert.equal(await count("payments"), 1);
    assert.equal(await count("receipts"), 1);
    assert.equal((await period("PREV")).amountPaid, 1000);
  });

  test("simultaneous callback and reconciliation record exactly one payment", async () => {
    await seed(1000);
    await Promise.all([applyStatus("REF1", "SUCCESSFUL", null), applyStatus("REF1", "SUCCESSFUL", null), applyStatus("REF1", "SUCCESSFUL", null)]);
    assert.equal(await count("payments"), 1);
    assert.equal((await period("PREV")).amountPaid, 1000);
    assert.equal((await period("CUR")).amountPaid, 400);
  });

  test("a failed payment records nothing", async () => {
    await seed(1000);
    await applyStatus("REF1", "FAILED", "Insufficient funds");
    const intent = (await db.doc("paymentIntents/REF1").get()).data()!;
    assert.equal(intent.status, "FAILED");
    assert.equal(intent.message, "Insufficient funds");
    assert.equal(await count("payments"), 0);
    assert.equal((await period("PREV")).amountPaid, 400);
  });

  test("a late FAILED does not undo a confirmed payment", async () => {
    await seed(1000);
    await applyStatus("REF1", "SUCCESSFUL", null);
    await applyStatus("REF1", "FAILED", "late");
    assert.equal((await db.doc("paymentIntents/REF1").get()).data()!.status, "SUCCESSFUL");
    assert.equal(await count("payments"), 1);
  });

  test("PENDING is a no-op and unknown references are ignored", async () => {
    await seed(1000);
    await applyStatus("REF1", "PENDING", null);
    await applyStatus("NOPE", "SUCCESSFUL", null);
    assert.equal((await db.doc("paymentIntents/REF1").get()).data()!.status, "PENDING");
    assert.equal(await count("payments"), 0);
  });
});
