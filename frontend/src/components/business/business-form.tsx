"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { BookkeepingResultCard } from "@/components/business/bookkeeping-result-card";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import {
  businessesApi,
  type Business,
  type BusinessIndustryGroup,
  type BusinessRequest,
} from "@/lib/api/businesses";

const businessSchema = z.object({
  name: z.string().min(1, "사업자명을 입력해주세요.").max(150),
  businessRegistrationNumber: z.string().max(30).optional(),
  industryName: z.string().max(150).optional(),
  industryGroup: z.enum(["GROUP_A", "GROUP_B", "GROUP_C", "UNKNOWN"]),
  professionalBusiness: z.boolean().default(false),
  openedOn: z.string().optional(),
  previousYearRevenue: z.preprocess(
    (value) => (value === "" || value === undefined ? undefined : Number(value)),
    z.number().min(0, "0 이상의 금액을 입력해주세요.").optional(),
  ),
});

type BusinessFormValues = z.infer<typeof businessSchema>;

const industryGroups: Array<{ value: BusinessIndustryGroup; label: string; help: string }> = [
  { value: "GROUP_A", label: "A그룹", help: "농업, 도소매업 등" },
  { value: "GROUP_B", label: "B그룹", help: "제조업, 음식점업, 건설업 등" },
  { value: "GROUP_C", label: "C그룹", help: "부동산임대업, 전문서비스업 등" },
  { value: "UNKNOWN", label: "모르겠음", help: "추가 확인 필요" },
];

export function BusinessForm({
  mode = "create",
  initialBusiness,
}: {
  mode?: "create" | "edit";
  initialBusiness?: Business;
}) {
  const [savedBusiness, setSavedBusiness] = useState<Business | null>(initialBusiness ?? null);
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BusinessFormValues>({
    resolver: zodResolver(businessSchema),
    defaultValues: {
      name: initialBusiness?.name ?? "",
      businessRegistrationNumber: initialBusiness?.businessRegistrationNumber ?? "",
      industryName: initialBusiness?.industryName ?? "",
      industryGroup: initialBusiness?.industryGroup ?? "GROUP_B",
      professionalBusiness: initialBusiness?.professionalBusiness ?? false,
      openedOn: initialBusiness?.openedOn ?? "",
      previousYearRevenue: initialBusiness?.previousYearRevenue ?? undefined,
    },
  });

  const mutation = useMutation({
    mutationFn: (request: BusinessRequest) =>
      mode === "edit" && initialBusiness
        ? businessesApi.update(initialBusiness.id, request)
        : businessesApi.create(request),
    onSuccess: (business) => {
      setSavedBusiness(business);
      void queryClient.invalidateQueries({ queryKey: ["businesses"] });
    },
  });

  function onSubmit(values: BusinessFormValues) {
    const request: BusinessRequest = {
      name: values.name,
      businessRegistrationNumber: values.businessRegistrationNumber || null,
      industryName: values.industryName || null,
      industryGroup: values.industryGroup,
      professionalBusiness: values.professionalBusiness,
      openedOn: values.openedOn || null,
      previousYearRevenue: values.previousYearRevenue ?? null,
    };
    mutation.mutate(request);
  }

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.error
        ? "저장 중 오류가 발생했습니다."
        : null;

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_420px]">
      <Card>
        <CardHeader>
          <CardTitle>{mode === "edit" ? "사업자 정보 수정" : "사업자 정보 등록"}</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-4" onSubmit={handleSubmit(onSubmit)}>
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="name">사업자명</Label>
                <Input id="name" {...register("name")} />
                {errors.name ? <p className="text-sm text-destructive">{errors.name.message}</p> : null}
              </div>
              <div className="space-y-2">
                <Label htmlFor="businessRegistrationNumber">사업자등록번호</Label>
                <Input
                  id="businessRegistrationNumber"
                  placeholder="선택 입력"
                  {...register("businessRegistrationNumber")}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="industryName">업종명</Label>
                <Input id="industryName" placeholder="예: 숙박 및 음식점업" {...register("industryName")} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="industryGroup">업종 그룹</Label>
                <select
                  id="industryGroup"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  {...register("industryGroup")}
                >
                  {industryGroups.map((group) => (
                    <option key={group.value} value={group.value}>
                      {group.label} - {group.help}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="openedOn">개업일</Label>
                <Input id="openedOn" type="date" {...register("openedOn")} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="previousYearRevenue">직전연도 수입금액</Label>
                <Input
                  id="previousYearRevenue"
                  type="number"
                  min="0"
                  step="10000"
                  placeholder="예: 120000000"
                  {...register("previousYearRevenue")}
                />
                {errors.previousYearRevenue ? (
                  <p className="text-sm text-destructive">{errors.previousYearRevenue.message}</p>
                ) : null}
              </div>
            </div>

            <label className="flex items-center gap-2 text-sm">
              <input className="h-4 w-4" type="checkbox" {...register("professionalBusiness")} />
              전문직 사업자에 해당합니다
            </label>

            {errorMessage ? <p className="text-sm text-destructive">{errorMessage}</p> : null}

            <Button disabled={mutation.isPending} type="submit">
              {mutation.isPending ? "저장 중" : "저장하고 예상 판정 보기"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {savedBusiness ? (
        <BookkeepingResultCard prediction={savedBusiness.bookkeepingPrediction} />
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>판별 결과</CardTitle>
          </CardHeader>
          <CardContent className="text-sm leading-6 text-muted-foreground">
            사업자 정보를 저장하면 입력값 기준 예상 판정 결과가 표시됩니다.
          </CardContent>
        </Card>
      )}
    </div>
  );
}

