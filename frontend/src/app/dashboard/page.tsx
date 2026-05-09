"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import type { ComponentType, ReactNode } from "react";
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  CircleDollarSign,
  ClipboardCheck,
  FileSpreadsheet,
  HelpCircle,
  ListChecks,
  Receipt,
  TrendingUp,
  Upload,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { ApiError } from "@/lib/api/client";
import { businessesApi } from "@/lib/api/businesses";
import { checklistApi, type ChecklistResponse } from "@/lib/api/checklist";
import { reportsApi, type ReportSummary } from "@/lib/api/reports";

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
  const year = new Date().getFullYear();
  const businessesQuery = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });
  const activeBusiness = businessesQuery.data?.[0];

  const yearlyReportQuery = useQuery({
    queryKey: ["reports", "yearly", activeBusiness?.id, year],
    queryFn: () => reportsApi.yearly(activeBusiness!.id, year),
    enabled: Boolean(activeBusiness?.id),
  });

  const checklistQuery = useQuery({
    queryKey: ["checklist", activeBusiness?.id],
    queryFn: () => checklistApi.get(activeBusiness!.id),
    enabled: Boolean(activeBusiness?.id),
  });

  const report = yearlyReportQuery.data?.report;
  const checklist = checklistQuery.data;
  const totalRevenue = report?.totalRevenue ?? 0;
  const totalExpense = report?.totalExpense ?? 0;
  const expectedIncome = report?.expectedIncome ?? 0;
  const unclassifiedCount = report?.unclassifiedTransactionCount ?? 0;
  const missingEvidenceCount = report?.missingEvidenceTransactionCount ?? 0;
  const progress = readinessProgress(Boolean(activeBusiness), checklist);
  const dashboardState = overallState(checklist, unclassifiedCount, missingEvidenceCount);
  const todoItems = buildTodoItems(Boolean(activeBusiness), report, checklist);
  const errorMessage = errorText(businessesQuery.error ?? yearlyReportQuery.error ?? checklistQuery.error);

  return (
    <TooltipProvider>
      <section className="space-y-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal">대시보드</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              오늘 확인할 숫자와 다음 할 일을 간단히 정리했습니다.
            </p>
          </div>
          <StatusBadge state={dashboardState} />
        </div>

        {!activeBusiness && !businessesQuery.isLoading ? (
          <Card>
            <CardContent className="flex flex-col gap-4 pt-6 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
              <span>등록된 사업자가 없습니다. 사업자 정보를 먼저 등록해주세요.</span>
              <Button asChild size="sm">
                <Link href="/onboarding">사업자 등록</Link>
              </Button>
            </CardContent>
          </Card>
        ) : null}
        {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}

        <div className="grid gap-4 xl:grid-cols-[minmax(0,1.2fr)_minmax(320px,0.8fr)]">
          <ReadinessCard
            checklist={checklist}
            progress={progress}
            state={dashboardState}
          />
          <NextTodoCard items={todoItems} />
        </div>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <MetricCard
            icon={CircleDollarSign}
            label="총매출"
            tooltip="업로드한 거래 중 입금액 또는 수입으로 읽힌 금액의 합계입니다."
            value={formatWon(totalRevenue)}
            state="정상"
          />
          <MetricCard
            icon={Receipt}
            label="총비용"
            tooltip="업로드한 거래 중 출금액 또는 비용으로 읽힌 금액의 합계입니다."
            value={formatWon(totalExpense)}
            state={totalExpense > totalRevenue && totalRevenue > 0 ? "주의" : "정상"}
          />
          <MetricCard
            icon={TrendingUp}
            label="예상 이익"
            tooltip="총매출에서 총비용을 뺀 금액입니다. 세금 계산 결과가 아니라 참고용입니다."
            value={formatWon(expectedIncome)}
            state={expectedIncome < 0 ? "주의" : "정상"}
          />
          <MetricCard
            icon={ListChecks}
            label="미분류 거래"
            tooltip="계정과목을 아직 정하지 못했거나 사용자의 확인이 필요한 거래입니다."
            value={`${unclassifiedCount}건`}
            state={unclassifiedCount > 0 ? "주의" : "정상"}
          />
          <MetricCard
            icon={AlertTriangle}
            label="증빙 누락"
            tooltip="영수증, 세금계산서, 카드전표 등 확인할 자료가 부족한 거래입니다."
            value={`${missingEvidenceCount}건`}
            state={missingEvidenceCount > 0 ? "위험" : "정상"}
          />
        </div>

        <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_360px]">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">리포트 요약</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 text-sm sm:grid-cols-3">
              <SummaryItem label="검토할 거래" value={`${unclassifiedCount}건`} state={unclassifiedCount > 0 ? "주의" : "정상"} />
              <SummaryItem label="증빙 확인" value={`${missingEvidenceCount}건`} state={missingEvidenceCount > 0 ? "위험" : "정상"} />
              <SummaryItem
                label="입금 차이 의심"
                value={`${report?.suspiciousItems.length ?? 0}건`}
                state={(report?.suspiciousItems.length ?? 0) > 0 ? "주의" : "정상"}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">체크리스트 상태</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <StatusRow label="위험" value={checklist?.dangerCount ?? 0} state="위험" />
              <StatusRow label="주의" value={checklist?.warningCount ?? 0} state="주의" />
              <StatusRow label="정상" value={checklist?.normalCount ?? 0} state="정상" />
            </CardContent>
          </Card>
        </div>
      </section>
    </TooltipProvider>
  );
}

function ReadinessCard({
  checklist,
  progress,
  state,
}: {
  checklist?: ChecklistResponse;
  progress: number;
  state: UiState;
}) {
  return (
    <Card>
      <CardHeader className="gap-3 pb-4 sm:flex-row sm:items-start sm:justify-between sm:space-y-0">
        <div>
          <CardTitle className="flex items-center gap-2 text-base sm:text-lg">
            신고 준비 진행률
            <HelpTooltip>
              체크리스트의 위험/주의 항목 수를 기준으로 단순 계산한 준비 상태입니다.
            </HelpTooltip>
          </CardTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            위험 항목을 먼저 줄이면 진행률이 빠르게 올라갑니다.
          </p>
        </div>
        <StatusBadge state={state} />
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="flex items-end justify-between gap-4">
          <div className="text-4xl font-semibold">{progress}%</div>
          <div className="text-right text-sm text-muted-foreground">
            전체 체크 {checklist?.totalCount ?? 0}건
          </div>
        </div>
        <Progress value={progress} />
        <div className="grid gap-3 text-sm sm:grid-cols-3">
          <CompactStatus label="위험" value={checklist?.dangerCount ?? 0} state="위험" />
          <CompactStatus label="주의" value={checklist?.warningCount ?? 0} state="주의" />
          <CompactStatus label="정상" value={checklist?.normalCount ?? 0} state="정상" />
        </div>
      </CardContent>
    </Card>
  );
}

function MetricCard({
  icon: Icon,
  label,
  value,
  state,
  tooltip,
}: {
  icon: ComponentType<{ className?: string }>;
  label: string;
  value: string;
  state: UiState;
  tooltip: ReactNode;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-3 space-y-0 pb-3">
        <CardTitle className="flex min-w-0 items-center gap-1 text-sm font-medium text-muted-foreground">
          <span className="truncate">{label}</span>
          <HelpTooltip>{tooltip}</HelpTooltip>
        </CardTitle>
        <Icon className="h-4 w-4 shrink-0 text-muted-foreground" />
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="break-keep text-2xl font-semibold sm:text-3xl">{value}</div>
        <StatusBadge state={state} />
      </CardContent>
    </Card>
  );
}

function NextTodoCard({ items }: { items: TodoItem[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">다음에 할 일</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {items.map((item) => (
          <TodoRow key={item.title} item={item} />
        ))}
      </CardContent>
    </Card>
  );
}

function TodoRow({ item }: { item: TodoItem }) {
  const Icon = item.icon;
  return (
    <div className="flex flex-col gap-3 rounded-md border p-3 sm:flex-row sm:items-center">
      <div className="flex min-w-0 flex-1 gap-3">
        <span className="mt-0.5 inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-secondary">
          <Icon className="h-4 w-4" />
        </span>
        <div className="min-w-0 space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="font-semibold tracking-normal">{item.title}</h2>
            <StatusBadge state={item.state} />
          </div>
          <p className="text-sm leading-6 text-muted-foreground">{item.description}</p>
        </div>
      </div>
      <Button asChild size="sm" variant="outline">
        <Link href={item.href}>
          {item.action}
          <ArrowRight className="h-4 w-4" />
        </Link>
      </Button>
    </div>
  );
}

function SummaryItem({ label, value, state }: { label: string; value: string; state: UiState }) {
  return (
    <div className="rounded-md border p-4">
      <div className="flex items-center justify-between gap-3">
        <span className="text-muted-foreground">{label}</span>
        <StatusBadge state={state} />
      </div>
      <div className="mt-3 text-xl font-semibold">{value}</div>
    </div>
  );
}

function StatusRow({ label, value, state }: { label: string; value: number; state: UiState }) {
  const active = value > 0;
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-muted-foreground">{label}</span>
      <Badge variant={active ? badgeVariant(state) : "secondary"}>{value}건</Badge>
    </div>
  );
}

function CompactStatus({ label, value, state }: { label: string; value: number; state: UiState }) {
  return (
    <div className="rounded-md border p-3">
      <div className="text-muted-foreground">{label}</div>
      <div className="mt-1 flex items-center justify-between gap-2">
        <span className="text-lg font-semibold">{value}건</span>
        <Badge variant={value > 0 ? badgeVariant(state) : "secondary"}>{label}</Badge>
      </div>
    </div>
  );
}

function HelpTooltip({ children }: { children: ReactNode }) {
  return (
    <Tooltip>
      <TooltipTrigger aria-label="설명 보기">
        <HelpCircle className="h-3.5 w-3.5 text-muted-foreground" />
      </TooltipTrigger>
      <TooltipContent>{children}</TooltipContent>
    </Tooltip>
  );
}

function StatusBadge({ state }: { state: UiState }) {
  if (state === "위험") {
    return <Badge variant="destructive">위험</Badge>;
  }
  if (state === "주의") {
    return <Badge variant="warning">주의</Badge>;
  }
  return (
    <Badge variant="success">
      <CheckCircle2 className="mr-1 h-3 w-3" />
      정상
    </Badge>
  );
}

function badgeVariant(state: UiState) {
  if (state === "위험") {
    return "destructive";
  }
  if (state === "주의") {
    return "warning";
  }
  return "success";
}

function readinessProgress(hasBusiness: boolean, checklist?: ChecklistResponse) {
  if (!hasBusiness) {
    return 0;
  }
  if (!checklist) {
    return 0;
  }
  const issuePenalty = checklist.dangerCount * 30 + checklist.warningCount * 15;
  return Math.max(10, Math.min(100, 100 - issuePenalty));
}

function overallState(
  checklist: ChecklistResponse | undefined,
  unclassifiedCount: number,
  missingEvidenceCount: number,
): UiState {
  if ((checklist?.dangerCount ?? 0) > 0 || missingEvidenceCount > 0) {
    return "위험";
  }
  if ((checklist?.warningCount ?? 0) > 0 || unclassifiedCount > 0) {
    return "주의";
  }
  return "정상";
}

function buildTodoItems(
  hasBusiness: boolean,
  report: ReportSummary | undefined,
  checklist: ChecklistResponse | undefined,
): TodoItem[] {
  if (!hasBusiness) {
    return [
      {
        title: "사업자 정보 등록",
        description: "업종과 직전연도 수입금액을 입력하면 장부 유형을 예상할 수 있습니다.",
        href: "/onboarding",
        action: "등록하기",
        state: "주의",
        icon: ClipboardCheck,
      },
    ];
  }

  const items: TodoItem[] = [];
  const hasNoTransactions = report && report.totalRevenue === 0 && report.totalExpense === 0;
  if (hasNoTransactions) {
    items.push({
      title: "거래 파일 업로드",
      description: "카드, 계좌, 매출 내역 CSV 또는 XLSX 파일을 올려주세요.",
      href: "/files",
      action: "업로드",
      state: "주의",
      icon: Upload,
    });
  }

  if ((report?.unclassifiedTransactionCount ?? 0) > 0) {
    items.push({
      title: "미분류 거래 정리",
      description: "확인 필요 거래에 계정과목을 선택하면 다음부터 더 쉽게 분류됩니다.",
      href: "/transactions",
      action: "분류하기",
      state: "주의",
      icon: ListChecks,
    });
  }

  if ((report?.missingEvidenceTransactionCount ?? 0) > 0) {
    items.push({
      title: "증빙 자료 확인",
      description: "영수증, 카드전표, 세금계산서가 빠진 거래를 먼저 확인해주세요.",
      href: "/checklist",
      action: "확인하기",
      state: "위험",
      icon: AlertTriangle,
    });
  }

  if ((report?.suspiciousItems.length ?? 0) > 0) {
    items.push({
      title: "매출과 입금 차이 확인",
      description: "사업 매출과 개인 입금이 섞였는지 체크리스트에서 확인해주세요.",
      href: "/checklist",
      action: "확인하기",
      state: "주의",
      icon: ClipboardCheck,
    });
  }

  if (items.length === 0 && checklist) {
    items.push({
      title: "간편장부 확인",
      description: "현재 자료 기준으로 장부와 신고 준비 리포트를 확인할 차례입니다.",
      href: "/ledger",
      action: "장부 보기",
      state: "정상",
      icon: FileSpreadsheet,
    });
  }

  return items.slice(0, 4);
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
