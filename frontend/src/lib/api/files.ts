import { apiRequest, authHeader } from "./client";

export type FileProcessingStatus = "UPLOADED" | "PARSING" | "PARSED" | "FAILED";

export type FileParseError = {
  rowNumber: number;
  message: string;
  rawData: string | null;
};

export type FileUploadResponse = {
  fileId: string;
  originalFilename: string;
  processingStatus: FileProcessingStatus;
  parsedCount: number;
  failedCount: number;
  errors: FileParseError[];
};

export const filesApi = {
  upload(businessId: string, file: File) {
    const formData = new FormData();
    formData.append("file", file);

    return apiRequest<FileUploadResponse>(`/businesses/${businessId}/files`, {
      method: "POST",
      headers: authHeader(),
      body: formData,
    });
  },
};

