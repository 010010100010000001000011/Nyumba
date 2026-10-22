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
}

export function allocatePayment(paymentAmount: number, unpaidPeriods: RentPeriodDoc[]): AllocationResult {
  let remaining = paymentAmount;
  const periods: RentPeriodDoc[] = [];
  const changed: RentPeriodDoc[] = [];

  // Oldest first
  const sorted = [...unpaidPeriods].sort((a, b) => a.year - b.year || a.month - b.month);

  for (const period of sorted) {
    const balance = period.amountDue - period.amountPaid;
    if (remaining <= 0 || balance <= 0) {
      periods.push(period);
      continue;
    }
    const toApply = Math.min(remaining, balance);
    const newPaid = round2(period.amountPaid + toApply);
    remaining = round2(remaining - toApply);
    const updated = {
      ...period,
      amountPaid: newPaid,
      status: newPaid >= period.amountDue ? "PAID" : "PARTIALLY_PAID",
    };
    periods.push(updated);
    changed.push(updated);
  }

  return { periods, changed, unallocated: Math.max(0, remaining) };
}

export function outstanding(periods: RentPeriodDoc[]): number {
  return round2(periods.reduce((sum, p) => sum + Math.max(0, p.amountDue - p.amountPaid), 0));
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}
