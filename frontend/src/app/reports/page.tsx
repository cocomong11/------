"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, BarChart3 } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { businessesApi } from "@/lib/api/businesses";
import { reportsApi } from "@/lib/api/reports";

export default function ReportsPage() {
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [year, setYear] = useState(new Date().getFullYear());

  const businessesQuery = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });
  const businessOptions = businessesQuery.data ?? [];
  const activeBusinessId = selectedBusinessId || businessOptions[0]?.id || "";

  const yearlyQuery = useQuery({
    queryKey: ["reports", "yearly", activeBusinessId, year],
    queryFn: () => reportsApi.yearly(activeBusinessId, year),
    enabled: Boolean(activeBusinessId),
  });
  const monthlyQuery = useQuery({
    queryKey: ["reports", "monthly", activeBusinessId, year],
    queryFn: () => reportsApi.monthly(activeBusinessId, year),
    enabled: Boolean(activeBusinessId),
  });

  const yearlyReport = yearlyQuery.data?.report;
  const monthlyReports = monthlyQuery.data?.reports ?? [];
  const errorMessage = errorText(businessesQuery.error ?? yearlyQuery.error ?? monthlyQuery.error);

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">신고 준비 리포트</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          월별/연간 수입, 비용, 예상 소득, 미분류 거래와 증빙 누락 상태를 참고용으로 정리합니다.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>조회 조건</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-[minmax(0,1fr)_160px]">
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
        </CardContent>
      </Card>

      {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="총수입" value={formatWon(yearlyReport?.totalRevenue ?? 0)} variant="success" />
        <MetricCard label="총비용" value={formatWon(yearlyReport?.totalExpense ?? 0)} variant="warning" />
        <MetricCard label="예상 소득" value={formatWon(yearlyReport?.expectedIncome ?? 0)} variant="secondary" />
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_380px]">
        <Card>
          <CardHeader>
            <CardTitle>월별 리포트</CardTitle>
          </CardHeader>
          <CardContent>
            {monthlyQuery.isLoading ? <p className="text-sm text-muted-foreground">리포트를 만드는 중입니다.</p> : null}
            {monthlyReports.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] border-collapse text-sm">
                  <thead>
                    <tr className="border-b text-left text-muted-foreground">
                      <th className="px-3 py-3 font-medium">월</th>
                      <th className="px-3 py-3 text-right font-medium">수입</th>
                      <th className="px-3 py-3 text-right font-medium">비용</th>
                      <th className="px-3 py-3 text-right font-medium">예상 소득</th>
                      <th className="px-3 py-3 text-right font-medium">미분류</th>
                      <th className="px-3 py-3 text-right font-medium">증빙 누락</th>
                    </tr>
                  </thead>
                  <tbody>
                    {monthlyReports.map((report) => (
                      <tr key={report.month} className="border-b">
                        <td className="px-3 py-3">{report.month}월</td>
                        <td className="px-3 py-3 text-right">{formatWon(report.totalRevenue)}</td>
                        <td className="px-3 py-3 text-right">{formatWon(report.totalExpense)}</td>
                        <td className="px-3 py-3 text-right">{formatWon(report.expectedIncome)}</td>
                        <td className="px-3 py-3 text-right">{report.unclassifiedTransactionCount}건</td>
                        <td className="px-3 py-3 text-right">{report.missingEvidenceTransactionCount}건</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>카테고리별 비용</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {yearlyReport?.categoryExpenses.length ? (
              yearlyReport.categoryExpenses.map((expense) => (
                <div key={expense.categoryName} className="flex items-center justify-between gap-3 rounded-md border p-3 text-sm">
                  <span className="truncate">{expense.categoryName}</span>
                  <span className="font-semibold">{formatWon(expense.amount)}</span>
                </div>
              ))
            ) : (
              <p className="text-sm text-muted-foreground">비용 카테고리 합계가 없습니다.</p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>검토 필요 항목</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <RiskLine label="미분류 거래" count={yearlyReport?.unclassifiedTransactionCount ?? 0} />
          <RiskLine label="증빙 누락 거래" count={yearlyReport?.missingEvidenceTransactionCount ?? 0} danger />
          {yearlyReport?.suspiciousItems.length ? (
            yearlyReport.suspiciousItems.map((item) => (
              <div key={item.title} className="rounded-md border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                <div className="flex items-center gap-2 font-semibold">
                  <AlertTriangle className="h-4 w-4" />
                  {item.title}
                </div>
                <p className="mt-2 leading-6">{item.description}</p>
                <p className="mt-1 font-medium">차이 금액 {formatWon(item.differenceAmount)}</p>
              </div>
            ))
          ) : (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <BarChart3 className="h-4 w-4" />
              매출 거래와 입금 거래 차이 의심 항목이 없습니다.
            </div>
          )}
          {yearlyReport?.notice ? (
            <p className="rounded-md bg-muted p-3 text-sm text-muted-foreground">{yearlyReport.notice}</p>
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
  value: string;
  variant: "success" | "warning" | "secondary";
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="text-3xl font-semibold">{value}</div>
        <Badge variant={variant}>연간 합계</Badge>
      </CardContent>
    </Card>
  );
}

function RiskLine({ label, count, danger = false }: { label: string; count: number; danger?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-md border p-3 text-sm">
      <span>{label}</span>
      <Badge variant={count > 0 ? (danger ? "destructive" : "warning") : "success"}>
        {count > 0 ? (danger ? "위험" : "주의") : "정상"} · {count}건
      </Badge>
    </div>
  );
}

function formatWon(value: number) {
  return `${new Intl.NumberFormat("ko-KR").format(value)}원`;
}

function errorText(error: unknown) {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return null;
}
