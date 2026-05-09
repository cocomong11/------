"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Save, Sparkles } from "lucide-react";
import { useMemo, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { businessesApi } from "@/lib/api/businesses";
import {
  transactionsApi,
  type ClassificationResult,
  type ClassificationStatus,
  type Transaction,
} from "@/lib/api/transactions";

const defaultCategories = [
  "소모품비",
  "광고선전비",
  "지급수수료",
  "차량유지비",
  "통신비",
  "여비교통비",
  "임차료",
  "수도광열비",
  "보험료 또는 복리후생비 후보",
  "접대비",
  "기타",
];

const statusLabel: Record<ClassificationStatus, string> = {
  AUTO_CLASSIFIED: "자동분류",
  NEEDS_REVIEW: "확인필요",
  USER_CONFIRMED: "사용자확정",
  UNCLASSIFIED: "미분류",
};

const statusVariant: Record<ClassificationStatus, "success" | "warning" | "secondary"> = {
  AUTO_CLASSIFIED: "success",
  NEEDS_REVIEW: "warning",
  USER_CONFIRMED: "success",
  UNCLASSIFIED: "secondary",
};

export default function TransactionsPage() {
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [reviewOnly, setReviewOnly] = useState(false);
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
    const categoryMatches = categoryFilter === "ALL" || transaction.categoryName === categoryFilter;
    const reviewMatches =
      !reviewOnly ||
      transaction.classificationStatus === "UNCLASSIFIED" ||
      transaction.classificationStatus === "NEEDS_REVIEW";
    return categoryMatches && reviewMatches;
  });

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

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">거래 내역</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          업로드된 거래를 확인하고 키워드 기반 자동 분류를 실행하거나 카테고리를 직접 수정합니다.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>분류 작업</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_220px_160px]">
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
              <Label htmlFor="categoryFilter">카테고리</Label>
              <select
                id="categoryFilter"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={categoryFilter}
                onChange={(event) => setCategoryFilter(event.target.value)}
              >
                <option value="ALL">전체</option>
                {categories.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-end">
              <label className="flex h-10 items-center gap-2 text-sm">
                <input
                  className="h-4 w-4"
                  checked={reviewOnly}
                  type="checkbox"
                  onChange={(event) => setReviewOnly(event.target.checked)}
                />
                미분류만
              </label>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Button
              disabled={!activeBusinessId || classifyMutation.isPending || transactions.length === 0}
              onClick={() => classifyMutation.mutate()}
              type="button"
            >
              <Sparkles className="h-4 w-4" />
              {classifyMutation.isPending ? "분류 중" : "일괄 자동 분류"}
            </Button>
            {lastClassification ? (
              <div className="text-sm text-muted-foreground">
                자동분류 {lastClassification.autoClassifiedCount}건 · 확인필요 {lastClassification.needsReviewCount}건
              </div>
            ) : null}
          </div>

          {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>거래 목록</CardTitle>
        </CardHeader>
        <CardContent>
          {transactionsQuery.isLoading ? (
            <p className="text-sm text-muted-foreground">거래를 불러오는 중입니다.</p>
          ) : null}
          {!transactionsQuery.isLoading && transactions.length === 0 ? (
            <p className="text-sm text-muted-foreground">업로드된 거래가 없습니다.</p>
          ) : null}
          {filteredTransactions.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[980px] border-collapse text-sm">
                <thead>
                  <tr className="border-b text-left text-muted-foreground">
                    <th className="px-3 py-3 font-medium">일자</th>
                    <th className="px-3 py-3 font-medium">거래처</th>
                    <th className="px-3 py-3 font-medium">내용</th>
                    <th className="px-3 py-3 text-right font-medium">금액</th>
                    <th className="px-3 py-3 text-right font-medium">부가세</th>
                    <th className="px-3 py-3 font-medium">상태</th>
                    <th className="px-3 py-3 font-medium">카테고리</th>
                    <th className="px-3 py-3 font-medium">수정</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredTransactions.map((transaction) => (
                    <tr key={transaction.id} className="border-b">
                      <td className="px-3 py-3">{transaction.transactionDate}</td>
                      <td className="px-3 py-3">{transaction.merchantName ?? "-"}</td>
                      <td className="max-w-[260px] truncate px-3 py-3">{transaction.description ?? "-"}</td>
                      <td className="px-3 py-3 text-right">
                        <span className={transaction.transactionType === "INCOME" ? "text-emerald-700" : ""}>
                          {transaction.transactionType === "INCOME" ? "+" : "-"}
                          {formatWon(transaction.amount)}
                        </span>
                      </td>
                      <td className="px-3 py-3 text-right">{formatWon(transaction.vatAmount)}</td>
                      <td className="px-3 py-3">
                        <Badge variant={statusVariant[transaction.classificationStatus]}>
                          {statusLabel[transaction.classificationStatus]}
                        </Badge>
                      </td>
                      <td className="px-3 py-3">
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
                      </td>
                      <td className="px-3 py-3">
                        <Button
                          disabled={updateMutation.isPending}
                          size="sm"
                          type="button"
                          variant="outline"
                          onClick={() => saveCategory(transaction)}
                        >
                          <Save className="h-4 w-4" />
                          저장
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function formatWon(value: number) {
  return new Intl.NumberFormat("ko-KR").format(value) + "원";
}
