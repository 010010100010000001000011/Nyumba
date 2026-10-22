import { initializeApp } from "firebase-admin/app";

initializeApp();

export { initiateLipilaPayment, lipilaCallback, reconcileLipilaPayments } from "./payments";
