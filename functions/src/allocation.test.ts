import { test } from "node:test";
import assert from "node:assert/strict";
import { allocatePayment, currentYearMonth, dueBalance, RentPeriodDoc, scheduleBalance } from "./allocation";
import { normalizeStatus, normalizeZambianPhone } from "./lipila";

// 15 Oct 2026, 12:00 in Lusaka
const NOW = Date.UTC(2026, 9, 15, 10, 0);

const period = (id: string, month: number, amountPaid = 0, year = 2026): RentPeriodDoc => ({
  periodId: id, year, month, amountDue: 2000, amountPaid, status: amountPaid > 0 ? "PARTIALLY_PAID" : "UNPAID",
});

test("allocates oldest period first and marks partial payments", () => {
  const r = allocatePayment(2500, [period("b", 11), period("a", 10)], NOW);
  assert.deepEqual(r.changed.map((p) => [p.periodId, p.amountPaid, p.status]), [
    ["a", 2000, "PAID"],
    ["b", 500, "PARTIALLY_PAID"],
  ]);
  assert.equal(r.unallocated, 0);
});

test("fully paid future months become ADVANCE_PAID", () => {
  const r = allocatePayment(4000, [period("oct", 10), period("nov", 11)], NOW);
  assert.deepEqual(r.changed.map((p) => p.status), ["PAID", "ADVANCE_PAID"]);
  assert.equal(r.advanceMonths, 1);
});

test("tops up an existing partial payment", () => {
  const r = allocatePayment(1500, [period("a", 10, 500)], NOW);
  assert.equal(r.changed[0].amountPaid, 2000);
  assert.equal(r.changed[0].status, "PAID");
});

test("reports money left over after all periods are paid", () => {
  assert.equal(allocatePayment(2600.5, [period("a", 10)], NOW).unallocated, 600.5);
});

test("due balance ignores future months; schedule balance does not", () => {
  const ps = [period("oct", 10), period("nov", 11), period("sep", 9, 1500)];
  assert.equal(dueBalance(ps, NOW), 2500);
  assert.equal(scheduleBalance(ps), 4500);
});

test("rent months follow Lusaka time, not UTC", () => {
  // 31 Oct 23:30 UTC is already 1 Nov in Lusaka (UTC+2)
  assert.deepEqual(currentYearMonth(Date.UTC(2026, 9, 31, 23, 30)), { year: 2026, month: 11 });
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
