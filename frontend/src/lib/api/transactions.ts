import { apiRequest, authHeader } from "./client";

export type TransactionType = "INCOME" | "EXPENSE";
export type ClassificationStatus = "AUTO_CLASSIFIED" | "NEEDS_REVIEW" | "USER_CONFIRMED" | "UNCLASSIFIED";
export type EvidenceStatus = "PRESENT" | "MISSING" | "NOT_REQUIRED" | "UNKNOWN";

export type Transaction = {
  id: string;
  businessId: string;
  uploadedFileId: string | null;
  transactionDate: string;
  merchantName: string | null;
  description: string | null;
  amount: number;
  vatAmount: number;
  transactionType: TransactionType;
  categoryName: string | null;
  classificationStatus: ClassificationStatus;
  evidenceStatus: EvidenceStatus;
  userMemo: string | null;
};

export type TransactionUpdateRequest = {
  categoryName: string | null;
  userMemo?: string | null;
};

export type ClassificationResult = {
  totalCount: number;
  autoClassifiedCount: number;
  needsReviewCount: number;
  transactions: Transaction[];
};

export const transactionsApi = {
  list(businessId: string) {
    return apiRequest<Transaction[]>(`/businesses/${businessId}/transactions`, {
      headers: authHeader(),
    });
  },
  classify(businessId: string) {
    return apiRequest<ClassificationResult>(`/businesses/${businessId}/classify-transactions`, {
      method: "POST",
      headers: authHeader(),
    });
  },
  update(id: string, request: TransactionUpdateRequest) {
    return apiRequest<Transaction>(`/transactions/${id}`, {
      method: "PATCH",
      headers: authHeader(),
      body: JSON.stringify(request),
    });
  },
};

