"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, ShieldAlert } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { businessesApi } from "@/lib/api/businesses";
import { checklistApi, type Severity } from "@/lib/api/checklist";

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
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">신고 준비 체크리스트</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          누락자료, 미분류 거래, 증빙 누락, 매출/입금 차이 의심 항목을 자동으로 정리합니다.
        </p>
      </div>

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
          {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-4">
        <CountCard label="전체" value={checklist?.totalCount ?? 0} variant="secondary" />
        <CountCard label="위험" value={checklist?.dangerCount ?? 0} variant="destructive" />
        <CountCard label="주의" value={checklist?.warningCount ?? 0} variant="warning" />
        <CountCard label="정상" value={checklist?.normalCount ?? 0} variant="success" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>확인 항목</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {checklistQuery.isLoading ? <p className="text-sm text-muted-foreground">체크리스트를 만드는 중입니다.</p> : null}
          {!checklistQuery.isLoading && checklist?.items.length === 0 ? (
            <p className="text-sm text-muted-foreground">표시할 체크리스트 항목이 없습니다.</p>
          ) : null}
          {checklist?.items.map((item) => (
            <div key={item.id} className="rounded-md border p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    {severityIcon(item.severity)}
                    <h2 className="font-semibold tracking-normal">{item.title}</h2>
                  </div>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">{item.description}</p>
                </div>
                <div className="flex shrink-0 gap-2">
                  <SeverityBadge severity={item.severity} />
                  <Badge variant="secondary">{item.status === "OPEN" ? "확인 필요" : item.status}</Badge>
                </div>
              </div>
            </div>
          ))}
          {checklist?.notice ? (
            <p className="rounded-md bg-muted p-3 text-sm text-muted-foreground">{checklist.notice}</p>
          ) : null}
        </CardContent>
      </Card>
    </section>
  );
}

function CountCard({
  label,
  value,
  variant,
}: {
  label: string;
  value: number;
  variant: "success" | "warning" | "destructive" | "secondary";
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="text-3xl font-semibold">{value}건</div>
        <Badge variant={variant}>{label}</Badge>
      </CardContent>
    </Card>
  );
}

function SeverityBadge({ severity }: { severity: Severity }) {
  if (severity === "DANGER") {
    return <Badge variant="destructive">위험</Badge>;
  }
  if (severity === "WARNING") {
    return <Badge variant="warning">주의</Badge>;
  }
  return <Badge variant="success">정상</Badge>;
}

function severityIcon(severity: Severity) {
  if (severity === "DANGER") {
    return <ShieldAlert className="h-4 w-4 text-destructive" />;
  }
  if (severity === "WARNING") {
    return <AlertTriangle className="h-4 w-4 text-amber-600" />;
  }
  return <CheckCircle2 className="h-4 w-4 text-emerald-600" />;
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
