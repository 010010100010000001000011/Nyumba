import { test } from "node:test";
import assert from "node:assert/strict";
import { allocatePayment, outstanding, RentPeriodDoc } from "./allocation";
import { normalizeStatus, normalizeZambianPhone } from "./lipila";

const period = (id: string, month: number, amountPaid = 0): RentPeriodDoc => ({
  periodId: id, year: 2026, month, amountDue: 2000, amountPaid, status: amountPaid > 0 ? "PARTIALLY_PAID" : "UNPAID",
});

test("allocates oldest period first and marks partial payments", () => {
  const r = allocatePayment(2500, [period("b", 11), period("a", 10)]);
  assert.deepEqual(r.changed.map((p) => [p.periodId, p.amountPaid, p.status]), [
    ["a", 2000, "PAID"],
    ["b", 500, "PARTIALLY_PAID"],
  ]);
  assert.equal(r.unallocated, 0);
  assert.equal(outstanding(r.periods), 1500);
});

test("tops up an existing partial payment", () => {
  const r = allocatePayment(1500, [period("a", 10, 500)]);
  assert.equal(r.changed[0].amountPaid, 2000);
  assert.equal(r.changed[0].status, "PAID");
});

test("reports money left over after all periods are paid", () => {
  const r = allocatePayment(2600.5, [period("a", 10)]);
  assert.equal(r.unallocated, 600.5);
});

test("normalizes Zambian mobile numbers", () => {
  assert.equal(normalizeZambianPhone("0977 123 456"), "260977123456");
  assert.equal(normalizeZambianPhone("+260961234567"), "260961234567");
  assert.equal(normalizeZambianPhone("771234567"), "260771234567");
  assert.equal(normalizeZambianPhone("0211234567"), null);
  assert.equal(normalizeZambianPhone("12345"), null);
});

test("normalizes Lipila status strings", () => {
  assert.equal(normalizeStatus("Successful"), "SUCCESSFUL");
  assert.equal(normalizeStatus("Failed"), "FAILED");
  assert.equal(normalizeStatus("Pending"), "PENDING");
  assert.equal(normalizeStatus(undefined), "PENDING");
});
