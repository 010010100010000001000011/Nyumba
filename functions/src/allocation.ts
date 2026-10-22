/**
 * Server-side port of the app's RentAllocationEngine
 * (app/src/main/java/.../domain/util/RentAllocationEngine.kt). Keep the two in sync.
 */
export interface RentPeriodDoc {
  periodId: string;
  year: number;
  month: number;
  amountDue: number;
  amountPaid: number;
  status: string;
}

export interface AllocationResult {
  periods: RentPeriodDoc[];
  /** Periods whose amountPaid/status changed and must be written back. */
  changed: RentPeriodDoc[];
  /** Amount left over after every period was settled. */
  unallocated: number;
  /** Months settled ahead of their month by this payment. */
  advanceMonths: number;
}

/** Rent months follow the tenants' calendar (Zambia, UTC+2), not the server's UTC clock. */
export const RENT_TIME_ZONE = "Africa/Lusaka";

export function currentYearMonth(now: number, timeZone = RENT_TIME_ZONE): { year: number; month: number } {
  const parts = new Intl.DateTimeFormat("en-US", { timeZone, year: "numeric", month: "numeric" }).formatToParts(new Date(now));
  return {
    year: Number(parts.find((p) => p.type === "year")!.value),
    month: Number(parts.find((p) => p.type === "month")!.value),
  };
}

function isFutureMonth(p: { year: number; month: number }, now: number): boolean {
  const { year, month } = currentYearMonth(now);
  return p.year > year || (p.year === year && p.month > month);
}

export function allocatePayment(paymentAmount: number, unpaidPeriods: RentPeriodDoc[], now = Date.now()): AllocationResult {
  let remaining = round2(paymentAmount);
  const periods: RentPeriodDoc[] = [];
  const changed: RentPeriodDoc[] = [];

  // Oldest first
  const sorted = [...unpaidPeriods].sort((a, b) => a.year - b.year || a.month - b.month);

  for (const period of sorted) {
    const balance = round2(period.amountDue - period.amountPaid);
    if (remaining <= 0 || balance <= 0) {
      periods.push(period);
      continue;
    }
    const toApply = Math.min(remaining, balance);
    const newPaid = round2(period.amountPaid + toApply);
    remaining = round2(remaining - toApply);
    const status =
      newPaid < period.amountDue ? "PARTIALLY_PAID" : isFutureMonth(period, now) ? "ADVANCE_PAID" : "PAID";
    const updated = { ...period, amountPaid: newPaid, status };
    periods.push(updated);
    changed.push(updated);
  }

  return {
    periods,
    changed,
    unallocated: Math.max(0, remaining),
    advanceMonths: changed.filter((p) => p.status === "ADVANCE_PAID").length,
  };
}

/** Outstanding rent already due (current and past months). */
export function dueBalance(periods: RentPeriodDoc[], now = Date.now()): number {
  return round2(periods.filter((p) => !isFutureMonth(p, now)).reduce((s, p) => s + Math.max(0, p.amountDue - p.amountPaid), 0));
}

/** Everything still owed on the schedule, including future months. */
export function scheduleBalance(periods: RentPeriodDoc[]): number {
  return round2(periods.reduce((s, p) => s + Math.max(0, p.amountDue - p.amountPaid), 0));
}

export function isSettled(status: string): boolean {
  return status === "PAID" || status === "ADVANCE_PAID";
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}
