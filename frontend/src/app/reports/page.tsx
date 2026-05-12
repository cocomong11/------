"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, BarChart3, CheckSquare, CircleDollarSign, Receipt, TrendingUp } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { PageTitle } from "@/components/app/page-title";
import { DataTable, ErrorState, LoadingState, StatCard, StatusBadge } from "@/components/common";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { businessesApi } from "@/lib/api/businesses";
import { ApiError } from "@/lib/api/client";
import { reportsApi, type ReportSummary } from "@/lib/api/reports";

type ReportTab = "yearly" | "monthly";

export default function ReportsPage() {
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [year, setYear] = useState(new Date().getFullYear());
  const [tab, setTab] = useState<ReportTab>("yearly");

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
  const monthlyReports = useMemo(() => monthlyQuery.data?.reports ?? [], [monthlyQuery.data?.reports]);
  const errorMessage = errorText(businessesQuery.error ?? yearlyQuery.error ?? monthlyQuery.error);
  const maxMonthlyAmount = useMemo(
    () =>
      Math.max(
        1,
        ...monthlyReports.flatMap((report) => [report.totalRevenue, report.totalExpense, Math.abs(report.expectedIncome)]),
      ),
    [monthlyReports],
  );

  return (
    <section className="space-y-6">
      <PageTitle
        eyebrow="신고 준비 리포트"
        title="리포트"
        description="매출, 비용, 예상이익과 주의 항목을 한 번에 확인합니다. 체크리스트와 연결해 빠진 자료를 정리할 수 있습니다."
        actions={
          <Button asChild variant="outline">
            <Link href="/checklist">
              <CheckSquare className="h-4 w-4" />
              체크리스트 보기
            </Link>
          </Button>
        }
      />

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
            <Input
              id="year"
              max={2100}
              min={2000}
              type="number"
              value={year}
              onChange={(event) => setYear(Number(event.target.value))}
            />
          </div>
        </CardContent>
      </Card>

      {errorMessage ? <ErrorState message={errorMessage} /> : null}

      <div className="grid gap-4 md:grid-cols-3">
        <StatCard icon={CircleDollarSign} label="총매출" value={formatWon(yearlyReport?.totalRevenue ?? 0)} status="연간 합계" tone="success" />
        <StatCard icon={Receipt} label="총비용" value={formatWon(yearlyReport?.totalExpense ?? 0)} status="연간 합계" tone="warning" />
        <StatCard icon={TrendingUp} label="예상이익" value={formatWon(yearlyReport?.expectedIncome ?? 0)} status="참고용" tone="info" />
      </div>

      <div className="inline-flex rounded-md border bg-card p-1">
        <TabButton active={tab === "yearly"} onClick={() => setTab("yearly")}>연간 요약</TabButton>
        <TabButton active={tab === "monthly"} onClick={() => setTab("monthly")}>월간 추이</TabButton>
      </div>

      {tab === "yearly" ? (
        <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_380px]">
          <YearlySummaryCard report={yearlyReport} loading={yearlyQuery.isLoading} />
          <CategoryExpenseCard report={yearlyReport} />
        </div>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>매출/비용/이익 차트</CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            {monthlyQuery.isLoading ? <LoadingState label="월간 리포트를 만드는 중입니다." /> : null}
            {monthlyReports.map((report) => (
              <MonthlyBar key={report.month} maxAmount={maxMonthlyAmount} report={report} />
            ))}
            {!monthlyQuery.isLoading ? (
              <DataTable
                columns={[
                  { key: "month", header: "월", cell: (report) => `${report.month}월` },
                  { key: "revenue", header: "매출", className: "text-right", cell: (report) => formatWon(report.totalRevenue) },
                  { key: "expense", header: "비용", className: "text-right", cell: (report) => formatWon(report.totalExpense) },
                  { key: "income", header: "예상이익", className: "text-right", cell: (report) => formatWon(report.expectedIncome) },
                  { key: "unclassified", header: "미분류", className: "text-right", cell: (report) => `${report.unclassifiedTransactionCount}건` },
                  { key: "evidence", header: "증빙 누락", className: "text-right", cell: (report) => `${report.missingEvidenceTransactionCount}건` },
                ]}
                emptyDescription="월간 리포트로 표시할 거래가 없습니다."
                emptyTitle="월간 데이터가 없습니다"
                getRowKey={(report) => String(report.month)}
                minWidth={820}
                rows={monthlyReports}
              />
            ) : null}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>검토 필요 항목</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <RiskLine danger label="증빙 누락 거래" count={yearlyReport?.missingEvidenceTransactionCount ?? 0} />
          <RiskLine label="미분류 거래" count={yearlyReport?.unclassifiedTransactionCount ?? 0} />
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
              매출 거래와 입금 거래 차이가 의심되는 항목이 없습니다.
            </div>
          )}
          <Alert>
            <AlertTitle>리포트 안내</AlertTitle>
            <AlertDescription>{yearlyReport?.notice ?? "현재 업로드된 자료를 기준으로 계산한 참고용 리포트입니다."}</AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    </section>
  );
}

function TabButton({
  active,
  children,
  onClick,
}: {
  active: boolean;
  children: React.ReactNode;
  onClick: () => void;
}) {
  return (
    <button
      className={[
        "h-9 rounded-md px-4 text-sm font-medium transition-colors",
        active ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-secondary hover:text-foreground",
      ].join(" ")}
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}

function YearlySummaryCard({ loading, report }: { loading: boolean; report?: ReportSummary }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>연간 요약</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {loading ? <LoadingState label="연간 리포트를 만드는 중입니다." /> : null}
        <SummaryRow label="총매출" value={formatWon(report?.totalRevenue ?? 0)} tone="success" />
        <SummaryRow label="총비용" value={formatWon(report?.totalExpense ?? 0)} tone="warning" />
        <SummaryRow label="예상이익" value={formatWon(report?.expectedIncome ?? 0)} tone="info" />
      </CardContent>
    </Card>
  );
}

function CategoryExpenseCard({ report }: { report?: ReportSummary }) {
  const max = Math.max(1, ...(report?.categoryExpenses.map((expense) => expense.amount) ?? [0]));
  return (
    <Card>
      <CardHeader>
        <CardTitle>카테고리별 비용 비중</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {report?.categoryExpenses.length ? (
          report.categoryExpenses.map((expense) => (
            <div key={expense.categoryName} className="space-y-2">
              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="truncate">{expense.categoryName}</span>
                <span className="font-semibold">{formatWon(expense.amount)}</span>
              </div>
              <div className="h-2 rounded-full bg-secondary">
                <div className="h-2 rounded-full bg-primary" style={{ width: `${Math.max(4, (expense.amount / max) * 100)}%` }} />
              </div>
            </div>
          ))
        ) : (
          <p className="text-sm leading-6 text-muted-foreground">비용 카테고리 합계가 없습니다.</p>
        )}
      </CardContent>
    </Card>
  );
}

function MonthlyBar({ maxAmount, report }: { maxAmount: number; report: ReportSummary }) {
  return (
    <div className="grid gap-3 rounded-md border p-4 md:grid-cols-[60px_1fr]">
      <div className="font-semibold">{report.month}월</div>
      <div className="space-y-2">
        <Bar label="매출" tone="success" value={report.totalRevenue} max={maxAmount} />
        <Bar label="비용" tone="warning" value={report.totalExpense} max={maxAmount} />
        <Bar label="이익" tone="info" value={Math.max(0, report.expectedIncome)} max={maxAmount} />
      </div>
    </div>
  );
}

function Bar({
  label,
  max,
  tone,
  value,
}: {
  label: string;
  max: number;
  tone: "success" | "warning" | "info";
  value: number;
}) {
  const color = tone === "success" ? "bg-emerald-500" : tone === "warning" ? "bg-amber-500" : "bg-blue-500";
  return (
    <div className="grid grid-cols-[52px_1fr_120px] items-center gap-3 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <div className="h-2 rounded-full bg-secondary">
        <div className={`h-2 rounded-full ${color}`} style={{ width: `${Math.max(2, (value / max) * 100)}%` }} />
      </div>
      <span className="text-right font-medium">{formatWon(value)}</span>
    </div>
  );
}

function SummaryRow({ label, tone, value }: { label: string; tone: "success" | "warning" | "info"; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-md border p-4">
      <span className="text-sm text-muted-foreground">{label}</span>
      <div className="flex items-center gap-3">
        <span className="font-semibold">{value}</span>
        <StatusBadge tone={tone}>확인</StatusBadge>
      </div>
    </div>
  );
}

function RiskLine({ label, count, danger = false }: { label: string; count: number; danger?: boolean }) {
  const tone = count > 0 ? (danger ? "danger" : "warning") : "success";
  return (
    <div className="flex items-center justify-between gap-3 rounded-md border p-3 text-sm">
      <span>{label}</span>
      <StatusBadge tone={tone}>{count > 0 ? (danger ? "위험" : "주의") : "정상"} · {count}건</StatusBadge>
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
