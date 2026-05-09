import { apiRequest, authHeader } from "./client";

export type BusinessIndustryGroup = "GROUP_A" | "GROUP_B" | "GROUP_C" | "UNKNOWN";

export type BookkeepingType = "SIMPLE_CANDIDATE" | "DOUBLE_ENTRY_REQUIRED" | "NEEDS_REVIEW";

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
  industryName: string | null;
  industryGroup: BusinessIndustryGroup;
  professionalBusiness: boolean;
  openedOn: string | null;
  previousYearRevenue: number | null;
  bookkeepingPrediction: BookkeepingPrediction;
};

export type BusinessRequest = {
  name: string;
  businessRegistrationNumber?: string | null;
  industryName?: string | null;
  industryGroup: BusinessIndustryGroup;
  professionalBusiness: boolean;
  openedOn?: string | null;
  previousYearRevenue?: number | null;
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
};

