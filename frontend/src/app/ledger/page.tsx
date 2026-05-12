"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { Download, FileSpreadsheet, Receipt, TrendingUp } from "lucide-react";
import { useMemo, useState } from "react";
import { PageTitle } from "@/components/app/page-title";
import { DataTable, ErrorState, LoadingState, StatCard, StatusBadge } from "@/components/common";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { businessesApi } from "@/lib/api/businesses";
import { ledgerApi } from "@/lib/api/ledger";
import type { EvidenceStatus } from "@/lib/api/transactions";

const evidenceLabel: Record<EvidenceStatus, string> = {
  PRESENT: "있음",
  MISSING: "누락",
  NOT_REQUIRED: "불필요",
  UNKNOWN: "미확인",
};

export default function LedgerPage() {
  const today = useMemo(() => new Date(), []);
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState<number | null>(today.getMonth() + 1);

  const businessesQuery = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });
  const businessOptions = businessesQuery.data ?? [];
  const activeBusinessId = selectedBusinessId || businessOptions[0]?.id || "";

  const ledgerQuery = useQuery({
    queryKey: ["ledger", activeBusinessId, year, month],
    queryFn: () => ledgerApi.getLedger({ businessId: activeBusinessId, year, month }),
    enabled: Boolean(activeBusinessId),
  });

  const downloadMutation = useMutation({
    mutationFn: () => ledgerApi.downloadExcel({ businessId: activeBusinessId, year, month }),
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = month ? `ledger-${year}-${String(month).padStart(2, "0")}.xlsx` : `ledger-${year}.xlsx`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    },
  });

  const errorMessage =
    ledgerQuery.error instanceof ApiError
      ? ledgerQuery.error.message
      : downloadMutation.error instanceof ApiError
        ? downloadMutation.error.message
        : businessesQuery.error instanceof ApiError
          ? businessesQuery.error.message
          : null;

  const entries = ledgerQuery.data?.entries ?? [];
  const summary = ledgerQuery.data?.summary;

  return (
    <section className="space-y-6">
      <PageTitle
        eyebrow="간편장부"
        title="장부 확인"
        description="업로드된 거래를 기준으로 수입과 비용을 정리합니다. 엑셀 다운로드로 신고 준비 자료를 따로 보관할 수 있습니다."
        actions={
          <Button
            disabled={!activeBusinessId || !entries.length || downloadMutation.isPending}
            onClick={() => downloadMutation.mutate()}
            type="button"
          >
            <Download className="h-4 w-4" />
            {downloadMutation.isPending ? "생성 중" : "엑셀 다운로드"}
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>조회 조건</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-[minmax(0,1fr)_160px_160px]">
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
          <div className="space-y-2">
            <Label htmlFor="year">연도</Label>
            <Input
              id="year"
              max={2100}
              min={2000}
              type="number"
              value={year}
              onChange={(event) => setYear(Number(event.target.value))}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="month">월</Label>
            <select
              id="month"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              value={month ?? "ALL"}
              onChange={(event) => setMonth(event.target.value === "ALL" ? null : Number(event.target.value))}
            >
              <option value="ALL">연간 전체</option>
              {Array.from({ length: 12 }, (_, index) => index + 1).map((monthValue) => (
                <option key={monthValue} value={monthValue}>
                  {monthValue}월
                </option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      {errorMessage ? <ErrorState message={errorMessage} /> : null}

      <div className="grid gap-4 md:grid-cols-3">
        <StatCard icon={Receipt} label="수입 합계" value={formatWon(summary?.totalRevenue ?? 0)} status="수입" tone="success" />
        <StatCard icon={FileSpreadsheet} label="비용 합계" value={formatWon(summary?.totalExpense ?? 0)} status="비용" tone="warning" />
        <StatCard icon={TrendingUp} label="차액" value={formatWon(summary?.netIncome ?? 0)} status="참고용" tone="info" />
      </div>

      <Alert>
        <AlertTitle>참고용 안내</AlertTitle>
        <AlertDescription>
          간편장부는 업로드된 자료를 기준으로 자동 정리한 참고 자료입니다. 누락된 매출, 현금 거래, 증빙 보관 여부는 직접 확인해주세요.
        </AlertDescription>
      </Alert>

      <Card>
        <CardHeader>
          <CardTitle>장부 테이블</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {ledgerQuery.isLoading ? <LoadingState label="장부를 생성하는 중입니다." /> : null}
          {!ledgerQuery.isLoading ? (
            <DataTable
              columns={[
                { key: "date", header: "일자", cell: (entry) => entry.entryDate },
                { key: "account", header: "계정과목", cell: (entry) => <span className="font-medium">{entry.accountTitle}</span> },
                { key: "summary", header: "내용", cell: (entry) => <span className="block max-w-[320px] truncate">{entry.summary}</span> },
                { key: "revenue", header: "수입", className: "text-right", cell: (entry) => formatWon(entry.revenueAmount) },
                { key: "expense", header: "비용", className: "text-right", cell: (entry) => formatWon(entry.expenseAmount) },
                { key: "evidence", header: "증빙", cell: (entry) => <EvidenceBadge status={entry.evidenceStatus} /> },
              ]}
              emptyDescription="해당 기간에 장부로 생성할 거래가 없습니다. 파일 업로드 또는 거래 분류 상태를 확인해주세요."
              emptyTitle="장부 항목이 없습니다"
              getRowKey={(entry) => entry.id}
              minWidth={920}
              rows={entries}
            />
          ) : null}
          {ledgerQuery.data?.notice ? (
            <p className="rounded-md bg-muted p-3 text-sm leading-6 text-muted-foreground">{ledgerQuery.data.notice}</p>
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function EvidenceBadge({ status }: { status: EvidenceStatus }) {
  if (status === "MISSING") {
    return <StatusBadge tone="warning">{evidenceLabel[status]}</StatusBadge>;
  }
  if (status === "PRESENT") {
    return <StatusBadge tone="success">{evidenceLabel[status]}</StatusBadge>;
  }
  return <StatusBadge tone="neutral">{evidenceLabel[status]}</StatusBadge>;
}

function formatWon(value: number) {
  return `${new Intl.NumberFormat("ko-KR").format(value)}원`;
}
