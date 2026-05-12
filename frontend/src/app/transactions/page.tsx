"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckSquare, Save, Search, Sparkles } from "lucide-react";
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
import {
  transactionsApi,
  type ClassificationResult,
  type ClassificationStatus,
  type EvidenceStatus,
  type Transaction,
} from "@/lib/api/transactions";

const defaultCategories = [
  "매출",
  "광고선전비",
  "지급수수료",
  "차량유지비",
  "통신비",
  "여비교통비",
  "임차료",
  "소모품비",
  "보험료",
  "접대비",
  "기타",
];

const statusLabel: Record<ClassificationStatus, string> = {
  AUTO_CLASSIFIED: "자동분류",
  NEEDS_REVIEW: "확인 필요",
  USER_CONFIRMED: "확정",
  UNCLASSIFIED: "미분류",
};

const evidenceLabel: Record<EvidenceStatus, string> = {
  PRESENT: "증빙 있음",
  MISSING: "증빙 누락",
  NOT_REQUIRED: "증빙 제외",
  UNKNOWN: "미확인",
};

export default function TransactionsPage() {
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [search, setSearch] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState<ClassificationStatus | "ALL">("ALL");
  const [reviewOnly, setReviewOnly] = useState(false);
  const [selectedRows, setSelectedRows] = useState<Set<string>>(new Set());
  const [draftCategories, setDraftCategories] = useState<Record<string, string>>({});
  const [lastClassification, setLastClassification] = useState<ClassificationResult | null>(null);
  const queryClient = useQueryClient();

  const businessesQuery = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });
  const businessOptions = businessesQuery.data ?? [];
  const activeBusinessId = selectedBusinessId || businessOptions[0]?.id || "";

  const transactionsQuery = useQuery({
    queryKey: ["transactions", activeBusinessId],
    queryFn: () => transactionsApi.list(activeBusinessId),
    enabled: Boolean(activeBusinessId),
  });

  const classifyMutation = useMutation({
    mutationFn: () => transactionsApi.classify(activeBusinessId),
    onSuccess: (result) => {
      setLastClassification(result);
      queryClient.setQueryData(["transactions", activeBusinessId], result.transactions);
      setSelectedRows(new Set());
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, categoryName }: { id: string; categoryName: string | null }) =>
      transactionsApi.update(id, { categoryName }),
    onSuccess: (updated) => {
      queryClient.setQueryData<Transaction[]>(["transactions", activeBusinessId], (current) =>
        current?.map((transaction) => (transaction.id === updated.id ? updated : transaction)) ?? [updated],
      );
    },
  });

  const transactions = useMemo(() => transactionsQuery.data ?? [], [transactionsQuery.data]);
  const categories = useMemo(() => {
    const fromTransactions = transactions
      .map((transaction) => transaction.categoryName)
      .filter((category): category is string => Boolean(category));
    return Array.from(new Set([...defaultCategories, ...fromTransactions])).sort((a, b) => a.localeCompare(b));
  }, [transactions]);

  const filteredTransactions = transactions.filter((transaction) => {
    const keyword = `${transaction.merchantName ?? ""} ${transaction.description ?? ""} ${transaction.categoryName ?? ""}`.toLowerCase();
    const searchMatches = !search || keyword.includes(search.toLowerCase());
    const fromMatches = !fromDate || transaction.transactionDate >= fromDate;
    const toMatches = !toDate || transaction.transactionDate <= toDate;
    const categoryMatches = categoryFilter === "ALL" || transaction.categoryName === categoryFilter;
    const statusMatches = statusFilter === "ALL" || transaction.classificationStatus === statusFilter;
    const reviewMatches =
      !reviewOnly ||
      transaction.classificationStatus === "UNCLASSIFIED" ||
      transaction.classificationStatus === "NEEDS_REVIEW";
    return searchMatches && fromMatches && toMatches && categoryMatches && statusMatches && reviewMatches;
  });

  const totalIncome = filteredTransactions
    .filter((transaction) => transaction.transactionType === "INCOME")
    .reduce((sum, transaction) => sum + transaction.amount, 0);
  const totalExpense = filteredTransactions
    .filter((transaction) => transaction.transactionType === "EXPENSE")
    .reduce((sum, transaction) => sum + transaction.amount, 0);
  const reviewCount = transactions.filter(
    (transaction) =>
      transaction.classificationStatus === "UNCLASSIFIED" ||
      transaction.classificationStatus === "NEEDS_REVIEW",
  ).length;

  const errorMessage =
    classifyMutation.error instanceof ApiError
      ? classifyMutation.error.message
      : updateMutation.error instanceof ApiError
        ? updateMutation.error.message
        : transactionsQuery.error instanceof ApiError
          ? transactionsQuery.error.message
          : businessesQuery.error instanceof ApiError
            ? businessesQuery.error.message
            : null;

  function updateDraft(transactionId: string, categoryName: string) {
    setDraftCategories((current) => ({ ...current, [transactionId]: categoryName }));
  }

  function saveCategory(transaction: Transaction) {
    const categoryName = draftCategories[transaction.id] ?? transaction.categoryName ?? "";
    updateMutation.mutate({
      id: transaction.id,
      categoryName: categoryName || null,
    });
  }

  function toggleAll(checked: boolean) {
    setSelectedRows(checked ? new Set(filteredTransactions.map((transaction) => transaction.id)) : new Set());
  }

  function toggleOne(transactionId: string, checked: boolean) {
    setSelectedRows((current) => {
      const next = new Set(current);
      if (checked) {
        next.add(transactionId);
      } else {
        next.delete(transactionId);
      }
      return next;
    });
  }

  return (
    <section className="space-y-6">
      <PageTitle
        eyebrow="거래 정리"
        title="거래 목록"
        description="업로드한 거래를 검색하고, 계정과목과 증빙 상태를 확인합니다. 미분류 거래부터 처리하면 신고 준비가 빨라집니다."
        actions={
          <Button
            disabled={!activeBusinessId || classifyMutation.isPending || transactions.length === 0}
            onClick={() => classifyMutation.mutate()}
            type="button"
          >
            <Sparkles className="h-4 w-4" />
            {classifyMutation.isPending ? "분류 중" : "자동 분류"}
          </Button>
        }
      />

      <div className="grid gap-4 md:grid-cols-4">
        <StatCard label="표시 거래" value={`${filteredTransactions.length}건`} status="조회 결과" tone="info" />
        <StatCard label="수입 합계" value={formatWon(totalIncome)} status="매출" tone="success" />
        <StatCard label="비용 합계" value={formatWon(totalExpense)} status="비용" tone="warning" />
        <StatCard label="확인 필요" value={`${reviewCount}건`} status={reviewCount > 0 ? "주의" : "정상"} tone={reviewCount > 0 ? "warning" : "success"} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>조회 조건</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 xl:grid-cols-[minmax(220px,1fr)_minmax(220px,1fr)_160px_160px]">
            <div className="space-y-2">
              <Label htmlFor="businessId">사업자</Label>
              <select
                id="businessId"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                disabled={businessesQuery.isLoading || businessOptions.length === 0}
                value={activeBusinessId}
                onChange={(event) => {
                  setSelectedBusinessId(event.target.value);
                  setLastClassification(null);
                  setSelectedRows(new Set());
                }}
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
              <Label htmlFor="search">검색</Label>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="search"
                  className="pl-9"
                  placeholder="거래처, 내용, 카테고리"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="fromDate">시작일</Label>
              <Input id="fromDate" type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="toDate">종료일</Label>
              <Input id="toDate" type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} />
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_220px_180px]">
            <div className="space-y-2">
              <Label htmlFor="categoryFilter">카테고리</Label>
              <select
                id="categoryFilter"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={categoryFilter}
                onChange={(event) => setCategoryFilter(event.target.value)}
              >
                <option value="ALL">전체 카테고리</option>
                {categories.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="statusFilter">상태</Label>
              <select
                id="statusFilter"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value as ClassificationStatus | "ALL")}
              >
                <option value="ALL">전체 상태</option>
                {Object.entries(statusLabel).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <label className="flex items-end gap-2 pb-2 text-sm">
              <input
                className="mb-1 h-4 w-4"
                checked={reviewOnly}
                type="checkbox"
                onChange={(event) => setReviewOnly(event.target.checked)}
              />
              미분류 거래만 보기
            </label>
          </div>

          {lastClassification ? (
            <Alert variant="success">
              <AlertTitle>자동 분류를 완료했습니다</AlertTitle>
              <AlertDescription>
                자동분류 {lastClassification.autoClassifiedCount}건, 확인 필요 {lastClassification.needsReviewCount}건입니다.
              </AlertDescription>
            </Alert>
          ) : null}
          {errorMessage ? <ErrorState message={errorMessage} /> : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="gap-3 sm:flex-row sm:items-center sm:justify-between sm:space-y-0">
          <div>
            <CardTitle>거래 테이블</CardTitle>
            <p className="mt-2 text-sm text-muted-foreground">선택 {selectedRows.size}건 · 인라인으로 카테고리를 수정할 수 있습니다.</p>
          </div>
          <Button disabled={selectedRows.size === 0} type="button" variant="outline">
            <CheckSquare className="h-4 w-4" />
            선택 확인 처리
          </Button>
        </CardHeader>
        <CardContent>
          {transactionsQuery.isLoading ? <LoadingState label="거래를 불러오는 중입니다." /> : null}
          {!transactionsQuery.isLoading ? (
            <DataTable
              columns={[
                {
                  key: "select",
                  header: (
                    <input
                      aria-label="전체 선택"
                      checked={filteredTransactions.length > 0 && selectedRows.size === filteredTransactions.length}
                      type="checkbox"
                      onChange={(event) => toggleAll(event.target.checked)}
                    />
                  ),
                  cell: (transaction) => (
                    <input
                      aria-label="거래 선택"
                      checked={selectedRows.has(transaction.id)}
                      type="checkbox"
                      onChange={(event) => toggleOne(transaction.id, event.target.checked)}
                    />
                  ),
                },
                { key: "date", header: "일자", cell: (transaction) => transaction.transactionDate },
                {
                  key: "merchant",
                  header: "거래처",
                  cell: (transaction) => <span className="font-medium">{transaction.merchantName ?? "-"}</span>,
                },
                {
                  key: "description",
                  header: "내용",
                  cell: (transaction) => <span className="block max-w-[260px] truncate">{transaction.description ?? "-"}</span>,
                },
                {
                  key: "amount",
                  header: "금액",
                  className: "text-right",
                  cell: (transaction) => (
                    <span className={transaction.transactionType === "INCOME" ? "font-medium text-emerald-700" : ""}>
                      {transaction.transactionType === "INCOME" ? "+" : "-"}
                      {formatWon(transaction.amount)}
                    </span>
                  ),
                },
                { key: "vat", header: "부가세", className: "text-right", cell: (transaction) => formatWon(transaction.vatAmount) },
                {
                  key: "status",
                  header: "분류 상태",
                  cell: (transaction) => <ClassificationBadge status={transaction.classificationStatus} />,
                },
                {
                  key: "evidence",
                  header: "증빙",
                  cell: (transaction) => <EvidenceBadge status={transaction.evidenceStatus} />,
                },
                {
                  key: "category",
                  header: "카테고리",
                  cell: (transaction) => (
                    <select
                      className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      value={draftCategories[transaction.id] ?? transaction.categoryName ?? ""}
                      onChange={(event) => updateDraft(transaction.id, event.target.value)}
                    >
                      <option value="">선택 필요</option>
                      {categories.map((category) => (
                        <option key={category} value={category}>
                          {category}
                        </option>
                      ))}
                    </select>
                  ),
                },
                {
                  key: "action",
                  header: "저장",
                  cell: (transaction) => (
                    <Button disabled={updateMutation.isPending} size="sm" type="button" variant="outline" onClick={() => saveCategory(transaction)}>
                      <Save className="h-4 w-4" />
                      저장
                    </Button>
                  ),
                },
              ]}
              emptyDescription="업로드된 거래가 없거나 조건에 맞는 거래가 없습니다."
              emptyTitle="거래가 없습니다"
              getRowKey={(transaction) => transaction.id}
              minWidth={1160}
              rows={filteredTransactions}
            />
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function ClassificationBadge({ status }: { status: ClassificationStatus }) {
  if (status === "AUTO_CLASSIFIED" || status === "USER_CONFIRMED") {
    return <StatusBadge tone="success">{statusLabel[status]}</StatusBadge>;
  }
  if (status === "NEEDS_REVIEW") {
    return <StatusBadge tone="warning">{statusLabel[status]}</StatusBadge>;
  }
  return <StatusBadge tone="neutral">{statusLabel[status]}</StatusBadge>;
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
