import { apiRequest, authHeader } from "./client";

export type ChecklistItemType =
  | "MISSING_DOCUMENT"
  | "UNCLASSIFIED_TRANSACTION"
  | "MISSING_EVIDENCE"
  | "BUSINESS_INFO_REVIEW"
  | "FILE_PARSE_ERROR";

export type ChecklistItemStatus = "OPEN" | "RESOLVED" | "IGNORED";

export type Severity = "INFO" | "WARNING" | "DANGER";

export type ChecklistItem = {
  id: string;
  transactionId: string | null;
  itemType: ChecklistItemType;
  status: ChecklistItemStatus;
  severity: Severity;
  title: string;
  description: string;
};

export type ChecklistResponse = {
  businessId: string;
  totalCount: number;
  dangerCount: number;
  warningCount: number;
  normalCount: number;
  items: ChecklistItem[];
  notice: string;
};

export const checklistApi = {
  get(businessId: string) {
    return apiRequest<ChecklistResponse>(`/businesses/${businessId}/checklist`, {
      headers: authHeader(),
    });
  },
};
