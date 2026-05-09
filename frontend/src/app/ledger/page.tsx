"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { Download } from "lucide-react";
import { useMemo, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { businessesApi } from "@/lib/api/businesses";
import { ledgerApi } from "@/lib/api/ledger";

const evidenceLabel = {
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
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">간편장부</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          거래 데이터를 기준으로 월별 또는 연간 간편장부를 생성하고 엑셀 파일로 받을 수 있습니다.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>조회 조건</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_160px_160px]">
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
              <input
                id="year"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
                onChange={(event) =>
                  setMonth(event.target.value === "ALL" ? null : Number(event.target.value))
                }
              >
                <option value="ALL">연간 전체</option>
                {Array.from({ length: 12 }, (_, index) => index + 1).map((monthValue) => (
                  <option key={monthValue} value={monthValue}>
                    {monthValue}월
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Button
              disabled={!activeBusinessId || !entries.length || downloadMutation.isPending}
              onClick={() => downloadMutation.mutate()}
              type="button"
            >
              <Download className="h-4 w-4" />
              {downloadMutation.isPending ? "생성 중" : "엑셀 다운로드"}
            </Button>
            {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="수입 합계" value={summary?.totalRevenue ?? 0} variant="success" />
        <MetricCard label="비용 합계" value={summary?.totalExpense ?? 0} variant="warning" />
        <MetricCard label="차액" value={summary?.netIncome ?? 0} variant="secondary" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>간편장부 테이블</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {ledgerQuery.isLoading ? <p className="text-sm text-muted-foreground">장부를 생성하는 중입니다.</p> : null}
          {!ledgerQuery.isLoading && entries.length === 0 ? (
            <p className="text-sm text-muted-foreground">해당 기간에 장부로 생성할 거래가 없습니다.</p>
          ) : null}
          {entries.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[860px] border-collapse text-sm">
                <thead>
                  <tr className="border-b text-left text-muted-foreground">
                    <th className="px-3 py-3 font-medium">일자</th>
                    <th className="px-3 py-3 font-medium">계정과목</th>
                    <th className="px-3 py-3 font-medium">내용</th>
                    <th className="px-3 py-3 text-right font-medium">수입</th>
                    <th className="px-3 py-3 text-right font-medium">비용</th>
                    <th className="px-3 py-3 font-medium">증빙</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map((entry) => (
                    <tr key={entry.id} className="border-b">
                      <td className="px-3 py-3">{entry.entryDate}</td>
                      <td className="px-3 py-3">{entry.accountTitle}</td>
                      <td className="max-w-[320px] truncate px-3 py-3">{entry.summary}</td>
                      <td className="px-3 py-3 text-right">{formatWon(entry.revenueAmount)}</td>
                      <td className="px-3 py-3 text-right">{formatWon(entry.expenseAmount)}</td>
                      <td className="px-3 py-3">
                        <Badge variant={entry.evidenceStatus === "MISSING" ? "warning" : "secondary"}>
                          {evidenceLabel[entry.evidenceStatus]}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                  <tr className="bg-muted font-semibold">
                    <td className="px-3 py-3">합계</td>
                    <td className="px-3 py-3" />
                    <td className="px-3 py-3" />
                    <td className="px-3 py-3 text-right">{formatWon(summary?.totalRevenue ?? 0)}</td>
                    <td className="px-3 py-3 text-right">{formatWon(summary?.totalExpense ?? 0)}</td>
                    <td className="px-3 py-3" />
                  </tr>
                </tbody>
              </table>
            </div>
          ) : null}
          {ledgerQuery.data?.notice ? (
            <p className="rounded-md bg-muted p-3 text-sm text-muted-foreground">{ledgerQuery.data.notice}</p>
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function MetricCard({
  label,
  value,
  variant,
}: {
  label: string;
  value: number;
  variant: "success" | "warning" | "secondary";
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="text-3xl font-semibold">{formatWon(value)}</div>
        <Badge variant={variant}>합계</Badge>
      </CardContent>
    </Card>
  );
}

function formatWon(value: number) {
  return new Intl.NumberFormat("ko-KR").format(value) + "원";
}
