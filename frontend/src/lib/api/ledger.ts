import { API_BASE_URL, ApiError, authHeader } from "./client";
import type { EvidenceStatus } from "./transactions";

export type LedgerEntry = {
  id: string;
  transactionId: string | null;
  entryDate: string;
  accountTitle: string;
  summary: string;
  revenueAmount: number;
  expenseAmount: number;
  evidenceStatus: EvidenceStatus;
};

export type LedgerSummary = {
  totalRevenue: number;
  totalExpense: number;
  netIncome: number;
};

export type LedgerResponse = {
  businessId: string;
  year: number;
  month: number | null;
  summary: LedgerSummary;
  entries: LedgerEntry[];
  notice: string;
};

export type LedgerQuery = {
  businessId: string;
  year: number;
  month?: number | null;
};

function queryString(query: Omit<LedgerQuery, "businessId">) {
  const params = new URLSearchParams({ year: String(query.year) });
  if (query.month) {
    params.set("month", String(query.month));
  }
  return params.toString();
}

export const ledgerApi = {
  async getLedger(query: LedgerQuery) {
    const response = await fetch(
      `${API_BASE_URL}/businesses/${query.businessId}/ledger?${queryString(query)}`,
      { headers: authHeader() },
    );

    if (!response.ok) {
      const errorBody = await response.json().catch(() => null);
      throw new ApiError(
        errorBody?.message ?? "간편장부를 불러오지 못했습니다.",
        response.status,
        errorBody?.code,
      );
    }

    return response.json() as Promise<LedgerResponse>;
  },

  async downloadExcel(query: LedgerQuery) {
    const response = await fetch(
      `${API_BASE_URL}/businesses/${query.businessId}/exports/ledger.xlsx?${queryString(query)}`,
      { headers: authHeader() },
    );

    if (!response.ok) {
      const errorBody = await response.json().catch(() => null);
      throw new ApiError(
        errorBody?.message ?? "엑셀 파일을 생성하지 못했습니다.",
        response.status,
        errorBody?.code,
      );
    }

    return response.blob();
  },
};

