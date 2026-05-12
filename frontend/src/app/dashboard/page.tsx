"use client";

import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  ArrowRight,
  Building2,
  CheckCircle2,
  CheckSquare,
  CircleDollarSign,
  FileSpreadsheet,
  MailCheck,
  Receipt,
  ShieldCheck,
  TrendingUp,
  Upload,
} from "lucide-react";
import Link from "next/link";
import { useState, type ComponentType } from "react";
import { PageTitle } from "@/components/app/page-title";
import { DataTable, ErrorState, LoadingState, StatCard, StatusBadge } from "@/components/common";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { authApi, type AuthUser } from "@/lib/api/auth";
import { businessesApi, type Business } from "@/lib/api/businesses";
import { ApiError } from "@/lib/api/client";
import { checklistApi, type ChecklistResponse } from "@/lib/api/checklist";
import { filesApi } from "@/lib/api/files";
import { reportsApi, type ReportSummary } from "@/lib/api/reports";
import { transactionsApi, type Transaction } from "@/lib/api/transactions";

type UiState = "정상" | "주의" | "위험";

type TodoItem = {
  title: string;
  description: string;
  href: string;
  action: string;
  state: UiState;
  icon: ComponentType<{ className?: string }>;
};

export default function DashboardPage() {
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [year, setYear] = useState(new Date().getFullYear());

  const meQuery = useQuery({ queryKey: ["auth", "me"], queryFn: authApi.me });
  const businessesQuery = useQuery({ queryKey: ["businesses"], queryFn: businessesApi.list });

  const businesses = businessesQuery.data ?? [];
  const activeBusinessId = selectedBusinessId || businesses[0]?.id || "";
  const activeBusiness = businesses.find((business) => business.id === activeBusinessId);

  const yearlyReportQuery = useQuery({
    queryKey: ["reports", "yearly", activeBusinessId, year],
    queryFn: () => reportsApi.yearly(activeBusinessId, year),
    enabled: Boolean(activeBusinessId),
  });
  const checklistQuery = useQuery({
    queryKey: ["checklist", activeBusinessId],
    queryFn: () => checklistApi.get(activeBusinessId),
    enabled: Boolean(activeBusinessId),
  });
  const filesQuery = useQuery({
    queryKey: ["files", activeBusinessId],
    queryFn: () => filesApi.list(activeBusinessId),
    enabled: Boolean(activeBusinessId),
  });
  const transactionsQuery = useQuery({
    queryKey: ["transactions", activeBusinessId],
    queryFn: () => transactionsApi.list(activeBusinessId),
    enabled: Boolean(activeBusinessId),
  });

  const report = yearlyReportQuery.data?.report;
  const checklist = checklistQuery.data;
  const loadingSummary = businessesQuery.isLoading || yearlyReportQuery.isLoading || checklistQuery.isLoading;
  const errorMessage = errorText(
    meQuery.error ??
      businessesQuery.error ??
      yearlyReportQuery.error ??
      checklistQuery.error ??
      filesQuery.error ??
      transactionsQuery.error,
  );
  const dashboardState = overallState(report, checklist);
  const progress = readinessProgress(Boolean(activeBusiness), report, checklist, activeBusiness);
  const todoItems = buildTodoItems(Boolean(activeBusiness), activeBusiness, loadingSummary, report, checklist);
  const recentFiles = (filesQuery.data ?? []).slice(0, 5);
  const recentTransactions = (transactionsQuery.data ?? []).slice(0, 5);

  return (
    <section className="space-y-6">
      <PageTitle
        eyebrow="오늘의 세무 상태"
        title="대시보드"
        description="신고 준비 상태, 빠진 증빙, 최근 업로드와 거래를 한 화면에서 확인합니다."
        actions={<StatusBadge tone={stateTone(dashboardState)}>{dashboardState}</StatusBadge>}
      />

      <TrustStatusCard activeBusiness={activeBusiness} user={meQuery.data} />

      <Card>
        <CardContent className="grid gap-4 pt-6 md:grid-cols-[minmax(0,1fr)_180px]">
          <div className="space-y-2">
            <Label htmlFor="dashboardBusinessId">사업자</Label>
            <select
              id="dashboardBusinessId"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              disabled={businessesQuery.isLoading || businesses.length === 0}
              value={activeBusinessId}
              onChange={(event) => setSelectedBusinessId(event.target.value)}
            >
              {businesses.length === 0 ? <option value="">등록된 사업자가 없습니다</option> : null}
              {businesses.map((business) => (
                <option key={business.id} value={business.id}>
                  {business.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="dashboardYear">신고 연도</Label>
            <Input
              id="dashboardYear"
              max={2100}
              min={2000}
              type="number"
              value={year}
              onChange={(event) => setYear(Number(event.target.value))}
            />
          </div>
        </CardContent>
      </Card>

      {errorMessage ? <ErrorState message={errorMessage} title="대시보드를 불러오지 못했습니다" /> : null}

      {!activeBusiness && !businessesQuery.isLoading ? (
        <EmptyBusinessCard />
      ) : (
        <>
          <div className="grid gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(320px,0.85fr)]">
            <ReadinessCard
              activeBusiness={activeBusiness}
              checklist={checklist}
              loading={loadingSummary}
              progress={progress}
              report={report}
              state={dashboardState}
            />
            <NextTodoCard items={todoItems} loading={loadingSummary} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            <StatCard icon={CircleDollarSign} label="총매출" value={formatWon(report?.totalRevenue ?? 0)} status="정상" tone="success" />
            <StatCard icon={Receipt} label="총비용" value={formatWon(report?.totalExpense ?? 0)} status="확인" tone="warning" />
            <StatCard icon={TrendingUp} label="예상이익" value={formatWon(report?.expectedIncome ?? 0)} status="참고용" tone="info" />
            <StatCard icon={CheckSquare} label="미분류 거래" value={`${report?.unclassifiedTransactionCount ?? 0}건`} status={(report?.unclassifiedTransactionCount ?? 0) > 0 ? "주의" : "정상"} tone={(report?.unclassifiedTransactionCount ?? 0) > 0 ? "warning" : "success"} />
            <StatCard icon={AlertTriangle} label="증빙 누락" value={`${report?.missingEvidenceTransactionCount ?? 0}건`} status={(report?.missingEvidenceTransactionCount ?? 0) > 0 ? "위험" : "정상"} tone={(report?.missingEvidenceTransactionCount ?? 0) > 0 ? "danger" : "success"} />
          </div>

          <div className="grid gap-4 xl:grid-cols-2">
            <RecentFilesCard loading={filesQuery.isLoading} rows={recentFiles} />
            <RecentTransactionsCard loading={transactionsQuery.isLoading} rows={recentTransactions} />
          </div>

          <Alert>
            <AlertTitle>쉬운 다음 단계</AlertTitle>
            <AlertDescription>
              사업자 검증, 파일 업로드, 미분류 거래 정리 순서로 진행하면 신고 준비 상태가 가장 빠르게 좋아집니다.
            </AlertDescription>
          </Alert>
        </>
      )}
    </section>
  );
}

function TrustStatusCard({ activeBusiness, user }: { activeBusiness?: Business; user?: AuthUser }) {
  const emailOk = Boolean(user?.emailVerified);
  const agreementOk = Boolean(user?.requiredAgreementsAccepted);
  const businessOk = activeBusiness?.verificationStatus === "VERIFIED";

  return (
    <Card>
      <CardContent className="grid gap-4 pt-6 md:grid-cols-4">
        <TrustItem description={user?.email ?? "로그인 정보를 확인 중입니다"} icon={MailCheck} ok={emailOk} title="이메일 인증" />
        <TrustItem description={activeBusiness?.name ?? "사업자 선택 필요"} icon={ShieldCheck} ok={businessOk} title="사업자 검증" />
        <TrustItem description="필수 동의 이력" icon={CheckSquare} ok={agreementOk} title="약관 동의" />
        <TrustItem
          description={user?.lastLoginAt ? formatDateTime(user.lastLoginAt) : "기록 없음"}
          icon={FileSpreadsheet}
          ok={Boolean(user?.lastLoginAt)}
          title="최근 로그인"
        />
      </CardContent>
    </Card>
  );
}

function TrustItem({
  description,
  icon: Icon,
  ok,
  title,
}: {
  description: string;
  icon: ComponentType<{ className?: string }>;
  ok: boolean;
  title: string;
}) {
  return (
    <div className="flex gap-3">
      <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-secondary">
        <Icon className="h-5 w-5" />
      </span>
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="font-semibold tracking-normal">{title}</h2>
          <StatusBadge tone={ok ? "success" : "warning"}>{ok ? "완료" : "확인 필요"}</StatusBadge>
        </div>
        <p className="mt-1 truncate text-sm text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}

function EmptyBusinessCard() {
  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-3">
          <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-secondary">
            <Building2 className="h-5 w-5" />
          </span>
          <div>
            <h2 className="font-semibold tracking-normal">사업자 정보가 필요합니다</h2>
            <p className="mt-1 text-sm leading-6 text-muted-foreground">
              사업자 정보를 등록하면 신고 준비 진행률과 다음 할 일을 볼 수 있습니다.
            </p>
          </div>
        </div>
        <Button asChild className="w-full sm:w-auto">
          <Link href="/onboarding/business">사업자 등록</Link>
        </Button>
      </CardContent>
    </Card>
  );
}

function ReadinessCard({
  activeBusiness,
  checklist,
  loading,
  progress,
  report,
  state,
}: {
  activeBusiness?: Business;
  checklist?: ChecklistResponse;
  loading: boolean;
  progress: number;
  report?: ReportSummary;
  state: UiState;
}) {
  return (
    <Card>
      <CardHeader className="gap-3 pb-4 sm:flex-row sm:items-start sm:justify-between sm:space-y-0">
        <div className="min-w-0">
          <CardTitle>신고 준비 진행률</CardTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            {activeBusiness?.name ?? "선택된 사업자"} · {report?.year ?? new Date().getFullYear()}년 자료 기준
          </p>
        </div>
        <StatusBadge tone={stateTone(state)}>{state}</StatusBadge>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="flex items-end justify-between gap-4">
          <div className="text-4xl font-semibold">{loading ? "--" : progress}%</div>
          <div className="text-right text-sm text-muted-foreground">체크 {loading ? "-" : checklist?.totalCount ?? 0}건</div>
        </div>
        <Progress aria-label="신고 준비 진행률" value={loading ? 0 : progress} />
        <div className="grid gap-3 text-sm sm:grid-cols-3">
          <CompactStatus label="위험" value={checklist?.dangerCount ?? 0} state="위험" />
          <CompactStatus label="주의" value={checklist?.warningCount ?? 0} state="주의" />
          <CompactStatus label="정상" value={checklist?.normalCount ?? 0} state="정상" />
        </div>
      </CardContent>
    </Card>
  );
}

function CompactStatus({ label, value, state }: { label: string; value: number; state: UiState }) {
  return (
    <div className="rounded-md border p-3">
      <div className="text-muted-foreground">{label}</div>
      <div className="mt-1 flex items-center justify-between gap-2">
        <span className="text-lg font-semibold">{value}건</span>
        <StatusBadge tone={value > 0 ? stateTone(state) : "neutral"}>{label}</StatusBadge>
      </div>
    </div>
  );
}

function NextTodoCard({ items, loading }: { items: TodoItem[]; loading: boolean }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>다음에 할 일</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {loading ? <LoadingState label="해야 할 일을 확인하고 있습니다." /> : null}
        {!loading && items.map((item) => <TodoRow key={item.title} item={item} />)}
      </CardContent>
    </Card>
  );
}

function TodoRow({ item }: { item: TodoItem }) {
  const Icon = item.icon;
  const done = item.state === "정상";

  return (
    <div className="flex flex-col gap-3 rounded-md border p-3 sm:flex-row sm:items-center">
      <div className="flex min-w-0 flex-1 gap-3">
        <span className="mt-0.5 inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md border bg-background">
          {done ? <CheckCircle2 className="h-4 w-4 text-emerald-600" /> : <Icon className="h-4 w-4" />}
        </span>
        <div className="min-w-0 space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="font-semibold tracking-normal">{item.title}</h2>
            <StatusBadge tone={stateTone(item.state)}>{item.state}</StatusBadge>
          </div>
          <p className="text-sm leading-6 text-muted-foreground">{item.description}</p>
        </div>
      </div>
      <Button asChild size="sm" variant={done ? "secondary" : "outline"}>
        <Link href={item.href}>
          {item.action}
          <ArrowRight className="h-4 w-4" />
        </Link>
      </Button>
    </div>
  );
}

function RecentFilesCard({ loading, rows }: { loading: boolean; rows: Array<{ id: string; originalFilename: string; processingStatus: string; uploadedAt: string }> }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>최근 업로드 파일</CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? <LoadingState label="파일 이력을 불러오는 중입니다." /> : null}
        {!loading ? (
          <DataTable
            columns={[
              { key: "name", header: "파일명", cell: (file) => <span className="font-medium">{file.originalFilename}</span> },
              { key: "status", header: "상태", cell: (file) => <StatusBadge tone={file.processingStatus === "PARSED" ? "success" : file.processingStatus === "FAILED" ? "danger" : "info"}>{file.processingStatus}</StatusBadge> },
              { key: "date", header: "업로드", cell: (file) => formatDateTime(file.uploadedAt) },
            ]}
            emptyDescription="최근 업로드한 파일이 없습니다."
            emptyTitle="파일 이력이 없습니다"
            getRowKey={(file) => file.id}
            minWidth={560}
            rows={rows}
          />
        ) : null}
      </CardContent>
    </Card>
  );
}

function RecentTransactionsCard({ loading, rows }: { loading: boolean; rows: Transaction[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>최근 거래</CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? <LoadingState label="최근 거래를 불러오는 중입니다." /> : null}
        {!loading ? (
          <DataTable
            columns={[
              { key: "date", header: "일자", cell: (transaction) => transaction.transactionDate },
              { key: "merchant", header: "거래처", cell: (transaction) => transaction.merchantName ?? "-" },
              { key: "amount", header: "금액", className: "text-right", cell: (transaction) => `${transaction.transactionType === "INCOME" ? "+" : "-"}${formatWon(transaction.amount)}` },
            ]}
            emptyDescription="최근 거래가 없습니다. 파일을 업로드하면 이곳에 표시됩니다."
            emptyTitle="거래가 없습니다"
            getRowKey={(transaction) => transaction.id}
            minWidth={560}
            rows={rows}
          />
        ) : null}
      </CardContent>
    </Card>
  );
}

function readinessProgress(
  hasBusiness: boolean,
  report?: ReportSummary,
  checklist?: ChecklistResponse,
  business?: Business,
) {
  if (!hasBusiness) {
    return 0;
  }
  let progress = business?.verificationStatus === "VERIFIED" ? 30 : 15;
  const hasUploadedData = Boolean(report && (report.totalRevenue > 0 || report.totalExpense > 0));
  if (hasUploadedData) {
    progress = 70;
  }
  const dangerPenalty = (checklist?.dangerCount ?? 0) * 25;
  const warningPenalty = (checklist?.warningCount ?? 0) * 10;
  const unclassifiedPenalty = Math.min((report?.unclassifiedTransactionCount ?? 0) * 5, 20);
  return Math.max(0, Math.min(100, progress + 30 - dangerPenalty - warningPenalty - unclassifiedPenalty));
}

function overallState(report?: ReportSummary, checklist?: ChecklistResponse): UiState {
  if ((checklist?.dangerCount ?? 0) > 0 || (report?.missingEvidenceTransactionCount ?? 0) > 0) {
    return "위험";
  }
  if ((checklist?.warningCount ?? 0) > 0 || (report?.unclassifiedTransactionCount ?? 0) > 0) {
    return "주의";
  }
  return "정상";
}

function stateTone(state: UiState) {
  if (state === "위험") {
    return "danger";
  }
  if (state === "주의") {
    return "warning";
  }
  return "success";
}

function buildTodoItems(
  hasBusiness: boolean,
  business: Business | undefined,
  loading: boolean,
  report?: ReportSummary,
  checklist?: ChecklistResponse,
): TodoItem[] {
  if (loading) {
    return [];
  }
  if (!hasBusiness) {
    return [{ title: "사업자 정보 등록", description: "상호명, 사업자등록번호, 대표자명, 개업일자를 먼저 입력해주세요.", href: "/onboarding/business", action: "등록하기", state: "주의", icon: Building2 }];
  }
  if (business?.verificationStatus !== "VERIFIED") {
    return [{ title: "사업자 검증", description: "파일 업로드 전에 사업자등록번호와 대표자 정보를 확인해주세요.", href: `/onboarding/business/verify?businessId=${business?.id ?? ""}`, action: "검증하기", state: "주의", icon: ShieldCheck }];
  }

  const items: TodoItem[] = [];
  const hasNoTransactions = !report || (report.totalRevenue === 0 && report.totalExpense === 0);
  if (hasNoTransactions) {
    items.push({ title: "거래 파일 업로드", description: "카드, 계좌, 매출 내역 CSV 또는 XLSX 파일을 올려주세요.", href: "/files", action: "업로드", state: "주의", icon: Upload });
  }
  if ((report?.missingEvidenceTransactionCount ?? 0) > 0 || (checklist?.dangerCount ?? 0) > 0) {
    items.push({ title: "증빙 자료 확인", description: "영수증, 카드전표, 세금계산서가 빠진 거래를 먼저 확인해주세요.", href: "/checklist", action: "확인하기", state: "위험", icon: AlertTriangle });
  }
  if ((report?.unclassifiedTransactionCount ?? 0) > 0) {
    items.push({ title: "미분류 거래 정리", description: "확인 필요 거래에 맞는 계정과목을 선택해주세요.", href: "/transactions", action: "분류하기", state: "주의", icon: CheckSquare });
  }
  if (items.length === 0) {
    items.push({ title: "간편장부 최종 확인", description: "현재 자료 기준으로 장부와 신고 준비 리포트를 한 번 더 확인하세요.", href: "/ledger", action: "장부 보기", state: "정상", icon: FileSpreadsheet });
  }
  return items.slice(0, 4);
}

function formatWon(value: number) {
  return `${new Intl.NumberFormat("ko-KR").format(value)}원`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
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
