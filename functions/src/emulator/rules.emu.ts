/**
 * Firestore security rules tests. Run with:  npm run test:emulator
 * Each test acts as a real user against firestore.rules in the emulator.
 */
import { after, before, beforeEach, describe, test } from "node:test";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  RulesTestEnvironment,
} from "@firebase/rules-unit-testing";

let env: RulesTestEnvironment;

// Cast: the compat Firestore types are not needed for these assertions
const as = (uid: string) => env.authenticatedContext(uid).firestore() as any;
const anon = () => env.unauthenticatedContext().firestore() as any;

before(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080").split(":");
  env = await initializeTestEnvironment({
    projectId: "demo-nyumba",
    firestore: { rules: readFileSync(resolve(__dirname, "../../../firestore.rules"), "utf8"), host, port: Number(port) },
  });
});

after(async () => {
  await env?.cleanup();
});

/**
 * World: landlord A owns house H1 (room R1); landlord B owns H2.
 * Tenant T lives in R1. Tenant U has no room. Tenant V lives in H2.
 */
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore() as any;
    const set = (path: string, data: object) => db.doc(path).set(data);
    await set("users/A", { uid: "A", role: "LANDLORD", fullName: "Landlord A" });
    await set("users/B", { uid: "B", role: "LANDLORD", fullName: "Landlord B" });
    await set("users/T", { uid: "T", role: "TENANT", fullName: "Tenant T", boardingHouseId: "H1", currentRoomId: "R1", landlordId: "A", monthlyRent: 1000, paymentDueDay: 5, identificationNumber: "123" });
    await set("users/U", { uid: "U", role: "TENANT", fullName: "Tenant U", boardingHouseId: null, currentRoomId: null, monthlyRent: 0 });
    await set("users/V", { uid: "V", role: "TENANT", fullName: "Tenant V", boardingHouseId: "H2", currentRoomId: "R2", landlordId: "B" });
    await set("boardingHouses/H1", { boardingHouseId: "H1", landlordId: "A", name: "House 1" });
    await set("boardingHouses/H2", { boardingHouseId: "H2", landlordId: "B", name: "House 2" });
    await set("rooms/R1", { roomId: "R1", boardingHouseId: "H1", capacity: 2, occupiedCount: 1, heldCount: 0, status: "AVAILABLE", monthlyRent: 1000 });
    await set("rooms/R2", { roomId: "R2", boardingHouseId: "H2", capacity: 1, occupiedCount: 1, heldCount: 0, status: "FULL", monthlyRent: 900 });
    await set("rentPeriods/P1", { periodId: "P1", tenantId: "T", boardingHouseId: "H1", month: 10, year: 2026, amountDue: 1000, amountPaid: 0, status: "UNPAID", sentReminders: [] });
    await set("payments/PAY1", { paymentId: "PAY1", tenantId: "T", boardingHouseId: "H1", amount: 1000, paymentMethod: "CASH" });
    await set("receipts/REC1", { receiptId: "REC1", tenantId: "T", boardingHouseId: "H1", amountPaid: 1000 });
    await set("roomRequests/RQ1", { requestId: "RQ1", tenantId: "U", boardingHouseId: "H1", roomId: "R1", status: "HOLDING" });
    await set("complaints/C1", { complaintId: "C1", tenantId: "T", boardingHouseId: "H1", status: "PENDING", title: "Leak" });
    await set("conversations/CV1", { conversationId: "CV1", participants: ["T", "A"], tenantId: "T", landlordId: "A", lastMessage: "" });
    await set("conversations/CV1/messages/M1", { messageId: "M1", senderId: "A", receiverId: "T", text: "hi", sentAt: 1 });
    await set("notifications/N1", { notificationId: "N1", tenantId: "T", title: "Hi", message: "m", isRead: false });
    await set("paymentIntents/PI1", { referenceId: "PI1", tenantId: "T", status: "PENDING" });
    await set("auditLogs/L1", { logId: "L1", boardingHouseId: "H1", action: "X" });
  });
});

describe("users", () => {
  test("anyone signing up can create only their own clean profile", async () => {
    await assertSucceeds(as("NEW").doc("users/NEW").set({ uid: "NEW", role: "TENANT", monthlyRent: 0, boardingHouseId: null, currentRoomId: null }));
    await assertFails(as("NEW").doc("users/OTHER").set({ uid: "OTHER", role: "TENANT" }));
    await assertFails(as("NEW").doc("users/NEW").set({ uid: "NEW", role: "ADMIN" }));
    await assertFails(as("NEW").doc("users/NEW").set({ uid: "NEW", role: "TENANT", boardingHouseId: "H1" }));
    await assertFails(as("NEW").doc("users/NEW").set({ uid: "NEW", role: "TENANT", monthlyRent: 1 }));
  });

  test("tenants edit their profile but not role, rent or tenancy", async () => {
    await assertSucceeds(as("T").doc("users/T").update({ fcmToken: "tok", phoneNumber: "0977" }));
    await assertFails(as("T").doc("users/T").update({ role: "LANDLORD" }));
    await assertFails(as("T").doc("users/T").update({ monthlyRent: 1 }));
    await assertFails(as("U").doc("users/U").update({ boardingHouseId: "H1", currentRoomId: "R1" }));
  });

  test("tenant profiles are visible only to themselves and their landlord", async () => {
    await assertSucceeds(as("A").doc("users/T").get());
    await assertFails(as("B").doc("users/T").get());
    await assertFails(as("U").doc("users/T").get());
    await assertSucceeds(as("T").doc("users/A").get()); // landlord profiles are public to signed-in users
    await assertFails(anon().doc("users/A").get());
  });

  test("landlord lists tenants of their own house only", async () => {
    await assertSucceeds(as("A").collection("users").where("boardingHouseId", "==", "H1").where("role", "==", "TENANT").get());
    await assertFails(as("B").collection("users").where("boardingHouseId", "==", "H1").where("role", "==", "TENANT").get());
  });

  test("landlord approves a tenant into their house; tenancy fields only", async () => {
    await assertSucceeds(as("A").doc("users/U").update({ boardingHouseId: "H1", currentRoomId: "R1", landlordId: "A", monthlyRent: 1000, paymentDueDay: 5 }));
    await assertFails(as("A").doc("users/T").update({ fullName: "Renamed" }));
    await assertFails(as("B").doc("users/T").update({ monthlyRent: 1 }));
  });
});

describe("houses and rooms", () => {
  test("only landlords create houses, and only for themselves", async () => {
    await assertSucceeds(as("A").collection("boardingHouses").add({ landlordId: "A", name: "New" }));
    await assertFails(as("A").collection("boardingHouses").add({ landlordId: "B", name: "Spoof" }));
    await assertFails(as("T").collection("boardingHouses").add({ landlordId: "T", name: "Tenant house" }));
    await assertFails(as("B").doc("boardingHouses/H1").update({ name: "Hijack" }));
    await assertFails(as("A").doc("boardingHouses/H1").update({ landlordId: "B" }));
  });

  test("only the owner manages rooms", async () => {
    await assertSucceeds(as("A").collection("rooms").add({ boardingHouseId: "H1", roomName: "R9" }));
    await assertFails(as("B").collection("rooms").add({ boardingHouseId: "H1", roomName: "R9" }));
    await assertSucceeds(as("A").doc("rooms/R1").update({ monthlyRent: 1200 }));
    await assertFails(as("T").doc("rooms/R1").update({ monthlyRent: 1 }));
  });

  test("a tenant hold reserves a bed only together with their own HOLDING request", async () => {
    const db = as("U");
    // What createRoomHold does: new HOLDING request + heldCount+1 naming it, atomically
    const ok = db.batch();
    ok.set(db.doc("roomRequests/NEW1"), { requestId: "NEW1", tenantId: "U", roomId: "R1", boardingHouseId: "H1", status: "HOLDING" });
    ok.update(db.doc("rooms/R1"), { heldCount: 1, updatedAt: 1, lastRequestId: "NEW1" });
    await assertSucceeds(ok.commit());

    // Attack: bump heldCount with no request (blocks the room for everyone)
    await assertFails(db.doc("rooms/R1").update({ heldCount: 2, updatedAt: 2 }));
    // Attack: reuse the same request to bump again
    await assertFails(db.doc("rooms/R1").update({ heldCount: 2, lastRequestId: "NEW1" }));
    // Attack: a HOLDING request that does not reserve the bed
    await assertFails(db.collection("roomRequests").doc("GHOST").set({ tenantId: "U", roomId: "R1", boardingHouseId: "H1", status: "HOLDING" }));
    // Attack: change other fields alongside
    const bad = db.batch();
    bad.set(db.doc("roomRequests/NEW2"), { tenantId: "U", roomId: "R1", boardingHouseId: "H1", status: "HOLDING" });
    bad.update(db.doc("rooms/R1"), { heldCount: 2, occupiedCount: 0, lastRequestId: "NEW2" });
    await assertFails(bad.commit());
  });

  test("a tenant releases only their own expiring hold", async () => {
    await env.withSecurityRulesDisabled((c) => (c.firestore() as any).doc("rooms/R1").update({ heldCount: 1 }));
    // Another tenant cannot release U's hold
    const t = as("T");
    const thief = t.batch();
    thief.update(t.doc("roomRequests/RQ1"), { status: "EXPIRED", updatedAt: 1 });
    thief.update(t.doc("rooms/R1"), { heldCount: 0, lastRequestId: "RQ1" });
    await assertFails(thief.commit());
    // What cleanupExpiredHolds does for the owner of the hold
    const db = as("U");
    const ok = db.batch();
    ok.update(db.doc("roomRequests/RQ1"), { status: "EXPIRED", updatedAt: 1 });
    ok.update(db.doc("rooms/R1"), { heldCount: 0, updatedAt: 1, lastRequestId: "RQ1" });
    await assertSucceeds(ok.commit());
  });
});

describe("room requests", () => {
  test("tenants request rooms for themselves as PENDING (or HOLDING via a hold) only", async () => {
    await assertSucceeds(as("U").collection("roomRequests").add({ tenantId: "U", boardingHouseId: "H1", roomId: "R1", status: "PENDING" }));
    await assertFails(as("U").collection("roomRequests").add({ tenantId: "T", boardingHouseId: "H1", roomId: "R1", status: "PENDING" }));
    await assertFails(as("U").collection("roomRequests").add({ tenantId: "U", boardingHouseId: "H1", roomId: "R1", status: "APPROVED" }));
  });

  test("tenants can expire, but not approve, their own hold", async () => {
    await assertSucceeds(as("U").doc("roomRequests/RQ1").update({ status: "EXPIRED", updatedAt: 1 }));
    await env.withSecurityRulesDisabled((c) => (c.firestore() as any).doc("roomRequests/RQ1").update({ status: "HOLDING" }));
    await assertFails(as("U").doc("roomRequests/RQ1").update({ status: "APPROVED" }));
  });

  test("landlord sees and decides requests for their house only", async () => {
    await assertSucceeds(as("A").collection("roomRequests").where("boardingHouseId", "==", "H1").get());
    await assertFails(as("B").collection("roomRequests").where("boardingHouseId", "==", "H1").get());
    await assertSucceeds(as("A").doc("roomRequests/RQ1").update({ status: "APPROVED" }));
    await assertFails(as("B").doc("roomRequests/RQ1").update({ status: "REJECTED" }));
  });

  test("tenant lists their own requests (dashboard + one-hold check)", async () => {
    await assertSucceeds(as("U").collection("roomRequests").where("tenantId", "==", "U").get());
    await assertSucceeds(as("U").collection("roomRequests").where("tenantId", "==", "U").where("status", "in", ["PENDING", "HOLDING"]).get());
    await assertFails(as("U").collection("roomRequests").where("tenantId", "==", "T").get());
  });
});

describe("money", () => {
  test("tenants cannot create payments or receipts (mobile money is server-side)", async () => {
    await assertFails(as("T").collection("payments").add({ tenantId: "T", boardingHouseId: "H1", amount: 1000, paymentMethod: "MOBILE_MONEY" }));
    await assertFails(as("T").collection("receipts").add({ tenantId: "T", boardingHouseId: "H1", amountPaid: 1000 }));
  });

  test("landlords record cash only, only in their house", async () => {
    await assertSucceeds(as("A").collection("payments").add({ tenantId: "T", boardingHouseId: "H1", amount: 500, paymentMethod: "CASH" }));
    await assertFails(as("A").collection("payments").add({ tenantId: "T", boardingHouseId: "H1", amount: 500, paymentMethod: "MOBILE_MONEY" }));
    await assertFails(as("B").collection("payments").add({ tenantId: "T", boardingHouseId: "H1", amount: 500, paymentMethod: "CASH" }));
    await assertSucceeds(as("A").collection("receipts").add({ tenantId: "T", boardingHouseId: "H1", amountPaid: 500 }));
  });

  test("payment history is private to the tenant and their landlord", async () => {
    await assertSucceeds(as("T").collection("payments").where("tenantId", "==", "T").get());
    await assertSucceeds(as("A").collection("payments").where("boardingHouseId", "==", "H1").get());
    await assertSucceeds(as("A").collection("payments").where("tenantId", "==", "T").get());
    await assertFails(as("U").collection("payments").where("tenantId", "==", "T").get());
    await assertFails(as("B").collection("payments").where("boardingHouseId", "==", "H1").get());
    await assertFails(as("V").doc("receipts/REC1").get());
  });

  test("tenants cannot mark their own rent as paid", async () => {
    await assertFails(as("T").doc("rentPeriods/P1").update({ amountPaid: 1000, status: "PAID" }));
    await assertFails(as("T").doc("rentPeriods/P1").update({ status: "PAID" }));
    // ...but the reminder job may flag overdue and record reminders
    await assertSucceeds(as("T").doc("rentPeriods/P1").update({ status: "OVERDUE", sentReminders: ["OVERDUE_1"], updatedAt: 1 }));
  });

  test("landlord allocates cash to rent periods in their house", async () => {
    await assertSucceeds(as("A").collection("rentPeriods").where("tenantId", "==", "T").where("boardingHouseId", "==", "H1").get());
    await assertSucceeds(as("A").doc("rentPeriods/P1").update({ amountPaid: 1000, status: "PAID", updatedAt: 1 }));
    await assertFails(as("B").doc("rentPeriods/P1").update({ amountPaid: 1000 }));
    await assertSucceeds(as("A").collection("rentPeriods").add({ tenantId: "U", boardingHouseId: "H1", month: 11, year: 2026 }));
    await assertFails(as("B").collection("rentPeriods").add({ tenantId: "U", boardingHouseId: "H1", month: 11, year: 2026 }));
  });

  test("payment intents are readable by their tenant only and never writable", async () => {
    await assertSucceeds(as("T").doc("paymentIntents/PI1").get());
    await assertFails(as("U").doc("paymentIntents/PI1").get());
    await assertFails(as("T").doc("paymentIntents/PI1").update({ status: "SUCCESSFUL" }));
    await assertFails(as("T").collection("paymentIntents").add({ tenantId: "T", status: "SUCCESSFUL" }));
  });
});

describe("complaints, chat, notifications, audit", () => {
  test("complaints: tenants file in their own house; owner resolves", async () => {
    await assertSucceeds(as("T").collection("complaints").add({ tenantId: "T", boardingHouseId: "H1", title: "x", status: "PENDING" }));
    await assertFails(as("T").collection("complaints").add({ tenantId: "T", boardingHouseId: "H2", title: "x" }));
    await assertFails(as("T").collection("complaints").add({ tenantId: "U", boardingHouseId: "H1", title: "x" }));
    await assertSucceeds(as("A").doc("complaints/C1").update({ status: "RESOLVED", landlordComment: "fixed" }));
    await assertFails(as("B").doc("complaints/C1").update({ status: "RESOLVED" }));
    await assertFails(as("V").doc("complaints/C1").get());
  });

  test("conversations are private to participants", async () => {
    await assertSucceeds(as("T").collection("conversations").where("participants", "array-contains", "T").get());
    await assertSucceeds(as("A").collection("conversations").where("tenantId", "==", "T").where("landlordId", "==", "A").get());
    await assertFails(as("U").doc("conversations/CV1").get());
    await assertSucceeds(as("A").collection("conversations").add({ participants: ["A", "U"], tenantId: "U", landlordId: "A" }));
    await assertFails(as("U").collection("conversations").add({ participants: ["A", "T"], tenantId: "T", landlordId: "A" }));
  });

  test("messages: participants read and send as themselves", async () => {
    await assertSucceeds(as("T").collection("conversations/CV1/messages").get());
    await assertFails(as("U").collection("conversations/CV1/messages").get());
    await assertSucceeds(as("T").collection("conversations/CV1/messages").add({ senderId: "T", receiverId: "A", text: "hello" }));
    await assertFails(as("T").collection("conversations/CV1/messages").add({ senderId: "A", receiverId: "T", text: "spoof" }));
    await assertSucceeds(as("T").doc("conversations/CV1/messages/M1").update({ readAt: 2 }));
    await assertFails(as("T").doc("conversations/CV1/messages/M1").update({ text: "edited" }));
    await assertSucceeds(as("T").doc("conversations/CV1").update({ lastMessage: "hello", lastMessageAt: 2 }));
  });

  test("notifications: recipients read and mark as read only", async () => {
    await assertSucceeds(as("T").collection("notifications").where("tenantId", "==", "T").get());
    await assertSucceeds(as("T").collection("notifications").where("tenantId", "==", "T").where("isRead", "==", false).get());
    await assertFails(as("U").collection("notifications").where("tenantId", "==", "T").get());
    await assertSucceeds(as("T").doc("notifications/N1").update({ isRead: true }));
    await assertFails(as("T").doc("notifications/N1").update({ message: "changed" }));
  });

  test("audit log: owner reads, nobody edits", async () => {
    await assertSucceeds(as("A").collection("auditLogs").where("boardingHouseId", "==", "H1").get());
    await assertFails(as("B").collection("auditLogs").where("boardingHouseId", "==", "H1").get());
    await assertFails(as("A").doc("auditLogs/L1").update({ action: "Y" }));
    await assertFails(as("A").doc("auditLogs/L1").delete());
  });

  test("landlord deletes a tenant's records (tenant purge)", async () => {
    const a = as("A");
    for (const col of ["payments", "receipts", "rentPeriods", "complaints", "notifications"]) {
      await assertSucceeds(a.collection(col).where("tenantId", "==", "T").get());
    }
    await assertSucceeds(a.collectionGroup("messages").where("receiverId", "==", "T").get());
    await assertSucceeds(a.doc("rentPeriods/P1").delete());
    await assertSucceeds(a.doc("conversations/CV1/messages/M1").delete());
    await assertFails(as("B").doc("receipts/REC1").delete());
    await assertFails(as("B").doc("users/T").delete());
    await assertSucceeds(a.doc("users/T").delete());
  });

  test("end to end: landlord records cash exactly as the app batches it", async () => {
    const db = as("A");
    const batch = db.batch();
    batch.set(db.doc("payments/NEWPAY"), { paymentId: "NEWPAY", tenantId: "T", boardingHouseId: "H1", amount: 1000, paymentMethod: "CASH", status: "SUCCESSFUL" });
    batch.update(db.doc("rentPeriods/P1"), { amountPaid: 1000, status: "PAID", updatedAt: 5 });
    batch.set(db.doc("receipts/NEWREC"), { receiptId: "NEWREC", tenantId: "T", boardingHouseId: "H1", amountPaid: 1000 });
    await assertSucceeds(batch.commit());
    await assertSucceeds(db.collection("notifications").add({ tenantId: "T", title: "Payment received", message: "m", isRead: false }));
    await assertSucceeds(db.collection("auditLogs").add({ boardingHouseId: "H1", action: "PAYMENT_RECORDED" }));
  });

  test("end to end: the tenant reminder job batch", async () => {
    const db = as("T");
    const batch = db.batch();
    batch.update(db.doc("rentPeriods/P1"), { status: "OVERDUE" });
    batch.set(db.doc("notifications/REM1"), { notificationId: "REM1", tenantId: "T", title: "Rent overdue", message: "m", isRead: false });
    batch.update(db.doc("rentPeriods/P1"), { sentReminders: ["OVERDUE_1"] });
    await assertSucceeds(batch.commit());
  });
});
