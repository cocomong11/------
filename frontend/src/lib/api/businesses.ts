import { apiRequest, authHeader } from "./client";

export type BusinessIndustryGroup = "GROUP_A" | "GROUP_B" | "GROUP_C" | "UNKNOWN";

export type BookkeepingType = "SIMPLE_CANDIDATE" | "DOUBLE_ENTRY_REQUIRED" | "NEEDS_REVIEW";

export type BusinessVerificationStatus = "NOT_STARTED" | "PENDING" | "VERIFIED" | "FAILED" | "NEEDS_REVIEW";

export type BookkeepingPrediction = {
  bookkeepingType: BookkeepingType;
  title: string;
  message: string;
  notice: string;
};

export type Business = {
  id: string;
  name: string;
  businessRegistrationNumber: string | null;
  representativeName: string | null;
  industryName: string | null;
  taxationType: string | null;
  industryGroup: BusinessIndustryGroup;
  professionalBusiness: boolean;
  hasEmployees: boolean;
  openedOn: string | null;
  previousYearRevenue: number | null;
  verificationStatus: BusinessVerificationStatus;
  bookkeepingPrediction: BookkeepingPrediction;
};

export type BusinessRequest = {
  name: string;
  businessRegistrationNumber?: string | null;
  representativeName?: string | null;
  industryName?: string | null;
  taxationType?: string | null;
  industryGroup: BusinessIndustryGroup;
  professionalBusiness: boolean;
  hasEmployees: boolean;
  openedOn?: string | null;
  previousYearRevenue?: number | null;
};

export type BusinessVerificationResponse = {
  businessId: string;
  verificationStatus: BusinessVerificationStatus;
  title: string;
  message: string;
  notice: string;
};

export const businessesApi = {
  create(request: BusinessRequest) {
    return apiRequest<Business>("/businesses", {
      method: "POST",
      headers: authHeader(),
      body: JSON.stringify(request),
    });
  },
  list() {
    return apiRequest<Business[]>("/businesses", {
      headers: authHeader(),
    });
  },
  get(id: string) {
    return apiRequest<Business>(`/businesses/${id}`, {
      headers: authHeader(),
    });
  },
  update(id: string, request: BusinessRequest) {
    return apiRequest<Business>(`/businesses/${id}`, {
      method: "PATCH",
      headers: authHeader(),
      body: JSON.stringify(request),
    });
  },
  verify(id: string, request: { businessRegistrationNumber: string; representativeName: string; openedOn: string }) {
    return apiRequest<BusinessVerificationResponse>(`/businesses/${id}/verify`, {
      method: "POST",
      headers: authHeader(),
      body: JSON.stringify(request),
    });
  },
};
