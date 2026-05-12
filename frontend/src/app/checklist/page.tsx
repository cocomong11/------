"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, ShieldAlert } from "lucide-react";
import { useState } from "react";
import { PageTitle } from "@/components/app/page-title";
import { DataTable, ErrorState, LoadingState, StatCard, StatusBadge } from "@/components/common";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { businessesApi } from "@/lib/api/businesses";
import { checklistApi, type Severity } from "@/lib/api/checklist";
import { ApiError } from "@/lib/api/client";

export default function ChecklistPage() {
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const businessesQuery = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });
  const businessOptions = businessesQuery.data ?? [];
  const activeBusinessId = selectedBusinessId || businessOptions[0]?.id || "";

  const checklistQuery = useQuery({
    queryKey: ["checklist", activeBusinessId],
    queryFn: () => checklistApi.get(activeBusinessId),
    enabled: Boolean(activeBusinessId),
  });

  const checklist = checklistQuery.data;
  const errorMessage = errorText(businessesQuery.error ?? checklistQuery.error);

  return (
    <section className="space-y-6">
      <PageTitle
        eyebrow="검토 항목"
        title="신고 준비 체크리스트"
        description="빠진 증빙, 미분류 거래, 매출과 입금 차이처럼 먼저 확인해야 할 항목을 정리합니다."
      />

      <Card>
        <CardHeader>
          <CardTitle>사업자 선택</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
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
          {errorMessage ? <ErrorState message={errorMessage} /> : null}
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-4">
        <StatCard label="전체" value={`${checklist?.totalCount ?? 0}건`} status="전체" tone="info" />
        <StatCard label="위험" value={`${checklist?.dangerCount ?? 0}건`} status="먼저 확인" tone="danger" />
        <StatCard label="주의" value={`${checklist?.warningCount ?? 0}건`} status="검토 필요" tone="warning" />
        <StatCard label="정상" value={`${checklist?.normalCount ?? 0}건`} status="문제 없음" tone="success" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>확인 항목</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {checklistQuery.isLoading ? <LoadingState label="체크리스트를 만드는 중입니다." /> : null}
          {!checklistQuery.isLoading ? (
            <DataTable
              columns={[
                {
                  key: "title",
                  header: "항목",
                  cell: (item) => (
                    <div className="flex items-start gap-2">
                      {severityIcon(item.severity)}
                      <div>
                        <div className="font-semibold">{item.title}</div>
                        <p className="mt-1 text-sm leading-6 text-muted-foreground">{item.description}</p>
                      </div>
                    </div>
                  ),
                },
                { key: "severity", header: "상태", cell: (item) => <SeverityBadge severity={item.severity} /> },
                { key: "status", header: "처리", cell: (item) => <StatusBadge tone={item.status === "OPEN" ? "warning" : "success"}>{item.status === "OPEN" ? "확인 필요" : "처리됨"}</StatusBadge> },
              ]}
              emptyDescription="표시할 체크리스트 항목이 없습니다."
              emptyTitle="검토 항목이 없습니다"
              getRowKey={(item) => item.id}
              rows={checklist?.items ?? []}
            />
          ) : null}
          {checklist?.notice ? (
            <p className="rounded-md bg-muted p-3 text-sm leading-6 text-muted-foreground">{checklist.notice}</p>
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function SeverityBadge({ severity }: { severity: Severity }) {
  if (severity === "DANGER") {
    return <StatusBadge tone="danger">위험</StatusBadge>;
  }
  if (severity === "WARNING") {
    return <StatusBadge tone="warning">주의</StatusBadge>;
  }
  return <StatusBadge tone="success">정상</StatusBadge>;
}

function severityIcon(severity: Severity) {
  if (severity === "DANGER") {
    return <ShieldAlert className="mt-1 h-4 w-4 text-destructive" />;
  }
  if (severity === "WARNING") {
    return <AlertTriangle className="mt-1 h-4 w-4 text-amber-600" />;
  }
  return <CheckCircle2 className="mt-1 h-4 w-4 text-emerald-600" />;
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
