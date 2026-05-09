"use client";

import { useQuery } from "@tanstack/react-query";
import { BusinessForm } from "@/components/business/business-form";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ApiError } from "@/lib/api/client";
import { businessesApi } from "@/lib/api/businesses";

const groupLabel = {
  GROUP_A: "A그룹",
  GROUP_B: "B그룹",
  GROUP_C: "C그룹",
  UNKNOWN: "미확인",
};

export default function BusinessesPage() {
  const query = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });

  const errorMessage =
    query.error instanceof ApiError
      ? query.error.message
      : query.error
        ? "사업자 목록을 불러오지 못했습니다."
        : null;

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">사업자 관리</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          로그인한 계정에 속한 사업자만 조회하고 관리합니다.
        </p>
      </div>

      <BusinessForm />

      <section className="space-y-4">
        <h2 className="text-lg font-semibold tracking-normal">내 사업자 목록</h2>
        {query.isLoading ? <p className="text-sm text-muted-foreground">불러오는 중입니다.</p> : null}
        {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}
        {query.data?.length === 0 ? (
          <p className="text-sm text-muted-foreground">등록된 사업자가 없습니다.</p>
        ) : null}
        <div className="grid gap-4 xl:grid-cols-2">
          {query.data?.map((business) => (
            <Card key={business.id}>
              <CardHeader className="space-y-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <CardTitle>{business.name}</CardTitle>
                    <p className="mt-2 text-sm text-muted-foreground">
                      {business.industryName ?? "업종명 미입력"} · {groupLabel[business.industryGroup]}
                    </p>
                  </div>
                  <Badge variant={business.professionalBusiness ? "warning" : "secondary"}>
                    {business.professionalBusiness ? "전문직" : "일반"}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-3 text-sm leading-6">
                <Badge
                  variant={
                    business.bookkeepingPrediction.bookkeepingType === "SIMPLE_CANDIDATE"
                      ? "success"
                      : business.bookkeepingPrediction.bookkeepingType === "DOUBLE_ENTRY_REQUIRED"
                        ? "warning"
                        : "secondary"
                  }
                >
                  {business.bookkeepingPrediction.title}
                </Badge>
                <p>{business.bookkeepingPrediction.message}</p>
                <p className="text-muted-foreground">{business.bookkeepingPrediction.notice}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>
    </section>
  );
}
