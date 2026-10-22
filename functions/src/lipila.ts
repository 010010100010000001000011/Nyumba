import { defineSecret, defineString } from "firebase-functions/params";

/**
 * Lipila configuration.
 *
 *   firebase functions:secrets:set LIPILA_API_KEY      (lsk_... sandbox, lpk_... production)
 *
 * LIPILA_BASE_URL / LIPILA_CALLBACK_URL are prompted for on first deploy, or set them in
 * functions/.env.<projectId>. Sandbox:    https://api.lipila.dev/api/v1
 *                                         Production: https://blz.lipila.io/api/v1
 */
export const LIPILA_API_KEY = defineSecret("LIPILA_API_KEY");
export const LIPILA_BASE_URL = defineString("LIPILA_BASE_URL", {
  default: "https://api.lipila.dev/api/v1",
});
/** URL of the deployed `lipilaCallback` function; leave empty to rely on reconciliation polling. */
export const LIPILA_CALLBACK_URL = defineString("LIPILA_CALLBACK_URL", { default: "" });

// VERIFY against https://docs.lipila.dev before going live: these paths, and whether
// `callbackUrl` belongs in the body (as the official Flutter SDK passes it) or in a header.
const PATHS = {
  mobileMoneyCollection: "/collections/mobile-money",
  transactionStatus: (referenceId: string) => `/transactions/${encodeURIComponent(referenceId)}`,
};

export type PaymentStatus = "PENDING" | "SUCCESSFUL" | "FAILED";

export interface LipilaResult {
  status: PaymentStatus;
  identifier: string | null;
  message: string | null;
}

export class LipilaError extends Error {
  constructor(readonly httpStatus: number, message: string) {
    super(message);
  }
}

/** Maps Lipila's status strings ("Successful", "Pending", "Failed", ...) onto ours. */
export function normalizeStatus(raw: unknown): PaymentStatus {
  const s = String(raw ?? "").toLowerCase();
  if (s.includes("success") || s === "completed" || s === "paid") return "SUCCESSFUL";
  if (/(fail|reject|declin|cancel|expire|error|reverse)/.test(s)) return "FAILED";
  return "PENDING";
}

/**
 * Normalises a Zambian mobile number to Lipila's account format (260XXXXXXXXX).
 * Accepts 0977123456, 977123456, +260977123456. Returns null if it is not a Zambian mobile.
 */
export function normalizeZambianPhone(input: string): string | null {
  const digits = input.replace(/\D/g, "");
  if (/^260[79]\d{8}$/.test(digits)) return digits;
  if (/^0[79]\d{8}$/.test(digits)) return "260" + digits.slice(1);
  if (/^[79]\d{8}$/.test(digits)) return "260" + digits;
  return null;
}

function parseResult(body: unknown): LipilaResult {
  // Responses may be flat or wrapped in `data`; read both.
  const obj = (body ?? {}) as Record<string, unknown>;
  const inner = (typeof obj.data === "object" && obj.data !== null ? obj.data : {}) as Record<string, unknown>;
  const pick = (key: string) => inner[key] ?? obj[key];
  const str = (v: unknown) => (v === undefined || v === null ? null : String(v));
  return {
    status: normalizeStatus(pick("status")),
    identifier: str(pick("identifier")),
    message: str(pick("message")),
  };
}

async function request(path: string, init: { method: "GET" | "POST"; body?: unknown }): Promise<LipilaResult> {
  const res = await fetch(LIPILA_BASE_URL.value().replace(/\/$/, "") + path, {
    method: init.method,
    headers: {
      "x-api-key": LIPILA_API_KEY.value(),
      "Accept": "application/json",
      ...(init.body !== undefined ? { "Content-Type": "application/json" } : {}),
    },
    body: init.body !== undefined ? JSON.stringify(init.body) : undefined,
    signal: AbortSignal.timeout(20_000),
  });
  const text = await res.text();
  let json: unknown = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    // non-JSON error body; surfaced below
  }
  if (!res.ok) {
    const message = (json as { message?: string } | null)?.message ?? text.slice(0, 300);
    throw new LipilaError(res.status, `Lipila ${res.status}: ${message}`);
  }
  return parseResult(json);
}

export function requestMobileMoneyCollection(params: {
  referenceId: string;
  amount: number;
  accountNumber: string;
  narration: string;
  email?: string;
}): Promise<LipilaResult> {
  const callbackUrl = LIPILA_CALLBACK_URL.value();
  return request(PATHS.mobileMoneyCollection, {
    method: "POST",
    body: {
      referenceId: params.referenceId,
      amount: params.amount,
      narration: params.narration,
      accountNumber: params.accountNumber,
      currency: "ZMW",
      ...(params.email ? { email: params.email } : {}),
      ...(callbackUrl ? { callbackUrl } : {}),
    },
  });
}

export function getTransactionStatus(referenceId: string): Promise<LipilaResult> {
  return request(PATHS.transactionStatus(referenceId), { method: "GET" });
}
