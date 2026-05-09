"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { AlertCircle, CheckCircle2, FileSpreadsheet, Upload } from "lucide-react";
import { DragEvent, useMemo, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { businessesApi } from "@/lib/api/businesses";
import { filesApi, type FileUploadResponse } from "@/lib/api/files";

export default function FilesPage() {
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<FileUploadResponse | null>(null);
  const [dragActive, setDragActive] = useState(false);

  const businessesQuery = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });

  const businessOptions = businessesQuery.data ?? [];
  const activeBusinessId = selectedBusinessId || businessOptions[0]?.id || "";

  const mutation = useMutation({
    mutationFn: () => {
      if (!activeBusinessId || !selectedFile) {
        throw new Error("사업자와 파일을 선택해주세요.");
      }
      return filesApi.upload(activeBusinessId, selectedFile);
    },
    onSuccess: (result) => {
      setUploadResult(result);
    },
  });

  const errorMessage = useMemo(() => {
    if (mutation.error instanceof ApiError) {
      return mutation.error.message;
    }
    if (mutation.error instanceof Error) {
      return mutation.error.message;
    }
    if (businessesQuery.error instanceof ApiError) {
      return businessesQuery.error.message;
    }
    return null;
  }, [businessesQuery.error, mutation.error]);

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault();
    setDragActive(false);
    const file = event.dataTransfer.files?.[0];
    if (file) {
      setSelectedFile(file);
      setUploadResult(null);
    }
  }

  function handleUpload() {
    mutation.mutate();
  }

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">파일 업로드</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          CSV 또는 XLSX 파일을 업로드하면 거래일자, 거래처, 내용, 입금액, 출금액, 부가세 컬럼을
          기본 매핑으로 파싱합니다.
        </p>
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <Card>
          <CardHeader>
            <CardTitle>거래 파일 선택</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="businessId">사업자</Label>
              <select
                id="businessId"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                disabled={businessesQuery.isLoading || businessOptions.length === 0}
                value={activeBusinessId}
                onChange={(event) => setSelectedBusinessId(event.target.value)}
              >
                {businessOptions.length === 0 ? <option value="">등록된 사업자가 없습니다</option> : null}
                {businessOptions.map((business) => (
                  <option key={business.id} value={business.id}>
                    {business.name}
                  </option>
                ))}
              </select>
            </div>

            <label
              className={[
                "flex min-h-56 cursor-pointer flex-col items-center justify-center gap-3 rounded-md border border-dashed p-6 text-center transition-colors",
                dragActive ? "border-primary bg-primary/5" : "border-border bg-card hover:bg-secondary/40",
              ].join(" ")}
              onDragEnter={(event) => {
                event.preventDefault();
                setDragActive(true);
              }}
              onDragOver={(event) => event.preventDefault()}
              onDragLeave={() => setDragActive(false)}
              onDrop={handleDrop}
            >
              <Upload className="h-8 w-8 text-muted-foreground" />
              <span className="text-sm font-medium">파일을 끌어오거나 클릭해서 선택</span>
              <span className="text-xs text-muted-foreground">CSV, XLSX · 최대 10MB</span>
              <input
                className="sr-only"
                type="file"
                accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                onChange={(event) => {
                  setSelectedFile(event.target.files?.[0] ?? null);
                  setUploadResult(null);
                }}
              />
            </label>

            {selectedFile ? (
              <div className="flex items-center justify-between gap-3 rounded-md border bg-muted p-3 text-sm">
                <span className="flex min-w-0 items-center gap-2">
                  <FileSpreadsheet className="h-4 w-4 shrink-0" />
                  <span className="truncate">{selectedFile.name}</span>
                </span>
                <Badge variant="secondary">{Math.ceil(selectedFile.size / 1024)}KB</Badge>
              </div>
            ) : null}

            {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}

            <Button
              disabled={!activeBusinessId || !selectedFile || mutation.isPending}
              onClick={handleUpload}
              type="button"
            >
              {mutation.isPending ? "업로드 중" : "업로드하고 파싱하기"}
            </Button>
          </CardContent>
        </Card>

        <UploadResultCard result={uploadResult} />
      </div>
    </section>
  );
}

function UploadResultCard({ result }: { result: FileUploadResponse | null }) {
  if (!result) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>업로드 결과</CardTitle>
        </CardHeader>
        <CardContent className="text-sm leading-6 text-muted-foreground">
          업로드가 완료되면 파싱 성공 건수와 실패 행 목록이 표시됩니다.
        </CardContent>
      </Card>
    );
  }

  const hasFailures = result.failedCount > 0;

  return (
    <Card>
      <CardHeader className="space-y-3">
        <div className="flex items-start justify-between gap-3">
          <CardTitle>업로드 결과</CardTitle>
          <Badge variant={result.processingStatus === "FAILED" ? "destructive" : hasFailures ? "warning" : "success"}>
            {result.processingStatus}
          </Badge>
        </div>
        <p className="text-sm text-muted-foreground">{result.originalFilename}</p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-md border p-4">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <CheckCircle2 className="h-4 w-4" />
              성공
            </div>
            <div className="mt-2 text-3xl font-semibold">{result.parsedCount}</div>
          </div>
          <div className="rounded-md border p-4">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <AlertCircle className="h-4 w-4" />
              실패
            </div>
            <div className="mt-2 text-3xl font-semibold">{result.failedCount}</div>
          </div>
        </div>

        {result.errors.length > 0 ? (
          <div className="space-y-2">
            <h2 className="text-sm font-semibold">파싱 실패 행</h2>
            <div className="max-h-72 space-y-2 overflow-auto">
              {result.errors.map((error) => (
                <div key={`${error.rowNumber}-${error.message}`} className="rounded-md border p-3 text-sm">
                  <div className="font-medium">{error.rowNumber}행 · {error.message}</div>
                  {error.rawData ? (
                    <div className="mt-1 truncate text-muted-foreground">{error.rawData}</div>
                  ) : null}
                </div>
              ))}
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">실패한 행이 없습니다.</p>
        )}
      </CardContent>
    </Card>
  );
}
