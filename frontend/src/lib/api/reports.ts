import { apiRequest, authHeader } from "./client";

export type CategoryExpense = {
  categoryName: string;
  amount: number;
};

export type SuspiciousItem = {
  title: string;
  description: string;
  differenceAmount: number;
};

export type ReportSummary = {
  year: number;
  month: number | null;
  totalRevenue: number;
  totalExpense: number;
  expectedIncome: number;
  categoryExpenses: CategoryExpense[];
  unclassifiedTransactionCount: number;
  missingEvidenceTransactionCount: number;
  suspiciousItems: SuspiciousItem[];
  notice: string;
};

export type MonthlyReportsResponse = {
  businessId: string;
  year: number;
  reports: ReportSummary[];
};

export type YearlyReportResponse = {
  businessId: string;
  report: ReportSummary;
};

export const reportsApi = {
  monthly(businessId: string, year: number) {
    return apiRequest<MonthlyReportsResponse>(`/businesses/${businessId}/reports/monthly?year=${year}`, {
      headers: authHeader(),
    });
  },
  yearly(businessId: string, year: number) {
    return apiRequest<YearlyReportResponse>(`/businesses/${businessId}/reports/yearly?year=${year}`, {
      headers: authHeader(),
    });
  },
};
