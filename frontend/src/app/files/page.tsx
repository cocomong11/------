"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { AlertCircle, CheckCircle2, Download, FileSpreadsheet, ShieldAlert, Upload } from "lucide-react";
import Link from "next/link";
import { DragEvent, useMemo, useState } from "react";
import { PageTitle } from "@/components/app/page-title";
import { DataTable, ErrorState, LoadingState, StatCard, StatusBadge } from "@/components/common";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { businessesApi } from "@/lib/api/businesses";
import { ApiError } from "@/lib/api/client";
import { filesApi, type FileUploadResponse } from "@/lib/api/files";

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const ALLOWED_EXTENSIONS = [".csv", ".xlsx"];

export default function FilesPage() {
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [uploadResult, setUploadResult] = useState<FileUploadResponse | null>(null);
  const [dragActive, setDragActive] = useState(false);

  const businessesQuery = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });

  const businessOptions = businessesQuery.data ?? [];
  const activeBusinessId = selectedBusinessId || businessOptions[0]?.id || "";
  const activeBusiness = businessOptions.find((business) => business.id === activeBusinessId);
  const canUpload = activeBusiness?.verificationStatus === "VERIFIED";

  const filesQuery = useQuery({
    queryKey: ["files", activeBusinessId],
    queryFn: () => filesApi.list(activeBusinessId),
    enabled: Boolean(activeBusinessId),
  });

  const mutation = useMutation({
    mutationFn: () => {
      if (!activeBusinessId || !selectedFile) {
        throw new Error("사업자와 파일을 선택해주세요.");
      }
      return filesApi.upload(activeBusinessId, selectedFile);
    },
    onSuccess: (result) => {
      setUploadResult(result);
      void filesQuery.refetch();
    },
  });

  const errorMessage = useMemo(() => {
    if (fileError) {
      return fileError;
    }
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
  }, [businessesQuery.error, fileError, mutation.error]);

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault();
    setDragActive(false);
    const file = event.dataTransfer.files?.[0];
    if (file) {
      chooseFile(file);
    }
  }

  function chooseFile(file: File) {
    const extension = file.name.toLowerCase().slice(file.name.lastIndexOf("."));
    if (!ALLOWED_EXTENSIONS.includes(extension)) {
      setFileError("CSV 또는 XLSX 파일만 업로드할 수 있습니다.");
      setSelectedFile(null);
      return;
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setFileError("파일 크기는 최대 10MB까지 업로드할 수 있습니다.");
      setSelectedFile(null);
      return;
    }
    setFileError(null);
    setSelectedFile(file);
    setUploadResult(null);
  }

  function handleUpload() {
    mutation.mutate();
  }

  function downloadSample() {
    const rows = [
      "거래일자,거래처,내용,입금액,출금액,부가세",
      "2026-01-03,예시카페,카드매출,120000,,10909",
      "2026-01-05,문구점,소모품 구입,,33000,3000",
    ];
    const blob = new Blob([`\uFEFF${rows.join("\n")}`], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "tax-upload-sample.csv";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  }

  return (
    <section className="space-y-6">
      <PageTitle
        eyebrow="자료 수집"
        title="파일 업로드"
        description="카드, 계좌, 매출 내역을 CSV 또는 XLSX로 올리면 거래와 장부 자료로 정리합니다."
        actions={
          <Button onClick={downloadSample} type="button" variant="outline">
            <Download className="h-4 w-4" />
            샘플 파일
          </Button>
        }
      />

      {!canUpload && activeBusiness ? (
        <Alert variant="warning">
          <AlertTitle>사업자 검증 후 업로드할 수 있습니다</AlertTitle>
          <AlertDescription>
            현재 사업자 검증 상태는 {activeBusiness.verificationStatus}입니다.{" "}
            <Link className="font-medium underline underline-offset-4" href={`/onboarding/business/verify?businessId=${activeBusiness.id}`}>
              사업자 검증으로 이동
            </Link>
          </AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <Card>
          <CardHeader>
            <CardTitle>업로드할 파일</CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
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
                "flex min-h-64 cursor-pointer flex-col items-center justify-center gap-3 rounded-md border border-dashed p-6 text-center transition-colors",
                dragActive ? "border-primary bg-primary/5" : "border-border bg-card hover:bg-secondary/50",
                !canUpload ? "opacity-70" : "",
              ].join(" ")}
              onDragEnter={(event) => {
                event.preventDefault();
                setDragActive(true);
              }}
              onDragOver={(event) => event.preventDefault()}
              onDragLeave={() => setDragActive(false)}
              onDrop={handleDrop}
            >
              {canUpload ? <Upload className="h-9 w-9 text-primary" /> : <ShieldAlert className="h-9 w-9 text-muted-foreground" />}
              <span className="text-base font-semibold">파일을 끌어오거나 클릭해서 선택</span>
              <span className="text-sm text-muted-foreground">CSV, XLSX · 최대 10MB · 사업자 검증 후 가능</span>
              <input
                className="sr-only"
                disabled={!canUpload}
                type="file"
                accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) {
                    chooseFile(file);
                  }
                }}
              />
            </label>

            <div className="grid gap-3 sm:grid-cols-3">
              <SupportChip label="CSV" />
              <SupportChip label="XLSX" />
              <SupportChip label="10MB 이하" />
            </div>

            {selectedFile ? (
              <div className="flex items-center justify-between gap-3 rounded-md border bg-muted p-3 text-sm">
                <span className="flex min-w-0 items-center gap-2">
                  <FileSpreadsheet className="h-4 w-4 shrink-0" />
                  <span className="truncate">{selectedFile.name}</span>
                </span>
                <Badge variant="secondary">{Math.ceil(selectedFile.size / 1024)}KB</Badge>
              </div>
            ) : null}

            {mutation.isPending ? (
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">업로드 및 분석 중</span>
                  <span className="font-medium">처리 중</span>
                </div>
                <Progress value={65} />
              </div>
            ) : null}

            {errorMessage ? <ErrorState message={errorMessage} title="업로드할 수 없습니다" /> : null}

            <Button
              disabled={!activeBusinessId || !selectedFile || !canUpload || mutation.isPending}
              onClick={handleUpload}
              type="button"
            >
              {mutation.isPending ? "업로드 중" : "업로드하고 분석하기"}
            </Button>
          </CardContent>
        </Card>

        <UploadResultCard result={uploadResult} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>최근 업로드 파일</CardTitle>
        </CardHeader>
        <CardContent>
          {filesQuery.isLoading ? <LoadingState label="업로드 이력을 불러오는 중입니다." /> : null}
          {!filesQuery.isLoading ? (
            <DataTable
              columns={[
                { key: "name", header: "파일명", cell: (file) => <span className="font-medium">{file.originalFilename}</span> },
                { key: "status", header: "상태", cell: (file) => <FileStatusBadge status={file.processingStatus} /> },
                { key: "size", header: "크기", className: "text-right", cell: (file) => `${Math.ceil(file.fileSizeBytes / 1024)}KB` },
                { key: "date", header: "업로드 일시", cell: (file) => formatDateTime(file.uploadedAt) },
              ]}
              emptyDescription="아직 업로드한 파일이 없습니다."
              emptyTitle="파일 이력이 없습니다"
              getRowKey={(file) => file.id}
              rows={filesQuery.data ?? []}
            />
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function SupportChip({ label }: { label: string }) {
  return <div className="rounded-md border bg-card px-3 py-2 text-center text-sm font-medium text-muted-foreground">{label}</div>;
}

function UploadResultCard({ result }: { result: FileUploadResponse | null }) {
  if (!result) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>파싱 결과 요약</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4 text-sm leading-6 text-muted-foreground">
          <p>업로드가 완료되면 성공 건수, 실패 건수, 실패 행이 표시됩니다.</p>
          <Alert>
            <AlertTitle>안전한 처리</AlertTitle>
            <AlertDescription>파일은 인증된 사용자와 검증된 사업자 기준으로만 처리됩니다.</AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  const hasFailures = result.failedCount > 0;

  return (
    <Card>
      <CardHeader className="space-y-3">
        <div className="flex items-start justify-between gap-3">
          <CardTitle>파싱 결과 요약</CardTitle>
          <FileStatusBadge status={result.processingStatus} />
        </div>
        <p className="break-all text-sm text-muted-foreground">{result.originalFilename}</p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <StatCard icon={CheckCircle2} label="성공" value={`${result.parsedCount}건`} status="정상" tone="success" />
          <StatCard icon={AlertCircle} label="실패" value={`${result.failedCount}건`} status={hasFailures ? "확인 필요" : "없음"} tone={hasFailures ? "warning" : "success"} />
        </div>

        {result.errors.length > 0 ? (
          <div className="space-y-2">
            <h2 className="text-sm font-semibold">분석 실패 행</h2>
            <div className="max-h-72 space-y-2 overflow-auto">
              {result.errors.map((error) => (
                <div key={`${error.rowNumber}-${error.message}`} className="rounded-md border p-3 text-sm">
                  <div className="font-medium">{error.rowNumber}행 · {error.message}</div>
                  {error.rawData ? <div className="mt-1 truncate text-muted-foreground">{error.rawData}</div> : null}
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

function FileStatusBadge({ status }: { status: string }) {
  if (status === "PARSED") {
    return <StatusBadge tone="success">분석 완료</StatusBadge>;
  }
  if (status === "FAILED") {
    return <StatusBadge tone="danger">실패</StatusBadge>;
  }
  if (status === "PARSING") {
    return <StatusBadge tone="info">분석 중</StatusBadge>;
  }
  return <StatusBadge tone="neutral">업로드됨</StatusBadge>;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
