"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Building2, HelpCircle, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useState, type ReactNode } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { BookkeepingResultCard } from "@/components/business/bookkeeping-result-card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { ApiError } from "@/lib/api/client";
import {
  businessesApi,
  type Business,
  type BusinessIndustryGroup,
  type BusinessRequest,
  type BusinessVerificationStatus,
} from "@/lib/api/businesses";

const businessSchema = z.object({
  name: z.string().min(1, "상호명을 입력해주세요.").max(150),
  businessRegistrationNumber: z
    .string()
    .transform((value) => value.replace(/\D/g, ""))
    .refine((value) => value.length === 10, "사업자등록번호 숫자 10자리를 입력해주세요."),
  representativeName: z.string().min(2, "대표자명을 입력해주세요.").max(100),
  openedOn: z.string().min(1, "개업일자를 선택해주세요."),
  industryName: z.string().max(150).optional(),
  taxationType: z.string().min(1, "과세유형을 선택해주세요."),
  industryGroup: z.enum(["GROUP_A", "GROUP_B", "GROUP_C", "UNKNOWN"]),
  professionalBusiness: z.boolean().default(false),
  hasEmployees: z.boolean().default(false),
  previousYearRevenue: z.preprocess(
    (value) => (value === "" || value === undefined ? undefined : Number(value)),
    z.number().min(0, "0 이상 금액을 입력해주세요.").optional(),
  ),
});

type BusinessFormValues = z.infer<typeof businessSchema>;

const industryGroups: Array<{ value: BusinessIndustryGroup; label: string; help: string }> = [
  { value: "GROUP_A", label: "A그룹", help: "농업, 도소매업 등 매출 기준이 높은 업종" },
  { value: "GROUP_B", label: "B그룹", help: "제조업, 음식점업, 건설업 등 일반 기준 업종" },
  { value: "GROUP_C", label: "C그룹", help: "부동산임대업, 전문서비스업 등 기준이 낮은 업종" },
  { value: "UNKNOWN", label: "모르겠음", help: "나중에 검토가 필요한 상태" },
];

const taxationTypes = ["일반과세자", "간이과세자", "면세사업자", "모르겠음"];

export function BusinessForm({
  mode = "create",
  initialBusiness,
}: {
  mode?: "create" | "edit";
  initialBusiness?: Business;
}) {
  const [savedBusiness, setSavedBusiness] = useState<Business | null>(initialBusiness ?? null);
  const queryClient = useQueryClient();
  const form = useForm<BusinessFormValues>({
    resolver: zodResolver(businessSchema),
    defaultValues: {
      name: initialBusiness?.name ?? "",
      businessRegistrationNumber: initialBusiness?.businessRegistrationNumber?.replace(/\D/g, "") ?? "",
      representativeName: initialBusiness?.representativeName ?? "",
      industryName: initialBusiness?.industryName ?? "",
      taxationType: initialBusiness?.taxationType ?? "일반과세자",
      industryGroup: initialBusiness?.industryGroup ?? "GROUP_B",
      professionalBusiness: initialBusiness?.professionalBusiness ?? false,
      hasEmployees: initialBusiness?.hasEmployees ?? false,
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
      businessRegistrationNumber: values.businessRegistrationNumber,
      representativeName: values.representativeName,
      industryName: values.industryName || null,
      taxationType: values.taxationType || null,
      industryGroup: values.industryGroup,
      professionalBusiness: values.professionalBusiness,
      hasEmployees: values.hasEmployees,
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
  const agreementRequired = mutation.error instanceof ApiError && mutation.error.code === "AGREEMENT_REQUIRED";

  return (
    <TooltipProvider>
      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <Card>
          <CardHeader className="space-y-3">
            <div className="flex items-center gap-2 text-sm font-medium text-primary">
              <Building2 className="h-4 w-4" />
              사업자 온보딩
            </div>
            <CardTitle>{mode === "edit" ? "사업자 정보 수정" : "사업자 정보 등록"}</CardTitle>
            <p className="text-sm leading-6 text-muted-foreground">
              입력값은 사업자 검증과 간편장부 대상 여부 예측에 함께 사용됩니다.
            </p>
          </CardHeader>
          <CardContent>
            <Form {...form}>
              <form className="grid gap-5" onSubmit={form.handleSubmit(onSubmit)}>
                <div className="grid gap-4 md:grid-cols-2">
                  <FormItem>
                    <Label htmlFor="name">상호명</Label>
                    <Input id="name" autoComplete="organization" {...form.register("name")} />
                    {form.formState.errors.name ? <FormMessage>{form.formState.errors.name.message}</FormMessage> : null}
                  </FormItem>
                  <FormItem>
                    <Label className="flex items-center gap-2" htmlFor="businessRegistrationNumber">
                      사업자등록번호
                      <HelpTooltip>화면에서는 일부만 보이고, 로그에는 전체값이 남지 않도록 처리합니다.</HelpTooltip>
                    </Label>
                    <Input
                      id="businessRegistrationNumber"
                      inputMode="numeric"
                      maxLength={12}
                      placeholder="숫자 10자리"
                      {...form.register("businessRegistrationNumber")}
                    />
                    {form.formState.errors.businessRegistrationNumber ? (
                      <FormMessage>{form.formState.errors.businessRegistrationNumber.message}</FormMessage>
                    ) : null}
                  </FormItem>
                  <FormItem>
                    <Label htmlFor="representativeName">대표자명</Label>
                    <Input id="representativeName" autoComplete="name" {...form.register("representativeName")} />
                    {form.formState.errors.representativeName ? (
                      <FormMessage>{form.formState.errors.representativeName.message}</FormMessage>
                    ) : null}
                  </FormItem>
                  <FormItem>
                    <Label htmlFor="openedOn">개업일자</Label>
                    <Input id="openedOn" type="date" {...form.register("openedOn")} />
                    {form.formState.errors.openedOn ? (
                      <FormMessage>{form.formState.errors.openedOn.message}</FormMessage>
                    ) : null}
                  </FormItem>
                  <FormItem>
                    <Label htmlFor="industryName">업종</Label>
                    <Input id="industryName" placeholder="예: 한식 음식점업" {...form.register("industryName")} />
                  </FormItem>
                  <FormItem>
                    <Label htmlFor="taxationType">과세유형</Label>
                    <select
                      id="taxationType"
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      {...form.register("taxationType")}
                    >
                      {taxationTypes.map((type) => (
                        <option key={type} value={type}>
                          {type}
                        </option>
                      ))}
                    </select>
                    {form.formState.errors.taxationType ? (
                      <FormMessage>{form.formState.errors.taxationType.message}</FormMessage>
                    ) : null}
                  </FormItem>
                  <FormItem>
                    <Label className="flex items-center gap-2" htmlFor="industryGroup">
                      업종 그룹
                      <HelpTooltip>간편장부 기준금액은 업종 그룹에 따라 달라집니다.</HelpTooltip>
                    </Label>
                    <select
                      id="industryGroup"
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      {...form.register("industryGroup")}
                    >
                      {industryGroups.map((group) => (
                        <option key={group.value} value={group.value}>
                          {group.label} - {group.help}
                        </option>
                      ))}
                    </select>
                  </FormItem>
                  <FormItem>
                    <Label className="flex items-center gap-2" htmlFor="previousYearRevenue">
                      직전연도 매출
                      <HelpTooltip>지난해 전체 매출입니다. 간편장부 대상 여부를 가늠하는 기준으로 사용합니다.</HelpTooltip>
                    </Label>
                    <Input
                      id="previousYearRevenue"
                      min="0"
                      placeholder="예: 120000000"
                      step="10000"
                      type="number"
                      {...form.register("previousYearRevenue")}
                    />
                    {form.formState.errors.previousYearRevenue ? (
                      <FormMessage>{form.formState.errors.previousYearRevenue.message}</FormMessage>
                    ) : null}
                  </FormItem>
                </div>

                <div className="grid gap-3 sm:grid-cols-2">
                  <label className="flex items-start gap-3 rounded-md border p-3 text-sm leading-6">
                    <input className="mt-1 h-4 w-4 shrink-0" type="checkbox" {...form.register("professionalBusiness")} />
                    <span>
                      전문직 사업자입니다
                      <span className="block text-xs text-muted-foreground">
                        변호사, 세무사, 의료업 등 일부 전문직은 기준이 다르게 적용될 수 있습니다.
                      </span>
                    </span>
                  </label>
                  <label className="flex items-start gap-3 rounded-md border p-3 text-sm leading-6">
                    <input className="mt-1 h-4 w-4 shrink-0" type="checkbox" {...form.register("hasEmployees")} />
                    <span>
                      직원이 있습니다
                      <span className="block text-xs text-muted-foreground">
                        원천세, 지급명세서 등 추가 확인이 필요할 수 있습니다.
                      </span>
                    </span>
                  </label>
                </div>

                {errorMessage ? (
                  <Alert variant={agreementRequired ? "warning" : "destructive"}>
                    <AlertTitle>{agreementRequired ? "약관 동의가 필요합니다" : "저장할 수 없습니다"}</AlertTitle>
                    <AlertDescription>{errorMessage}</AlertDescription>
                  </Alert>
                ) : null}

                <Button disabled={mutation.isPending} type="submit">
                  {mutation.isPending ? "저장 중" : "저장하고 검증 준비"}
                </Button>
              </form>
            </Form>
          </CardContent>
        </Card>

        <aside className="space-y-4">
          {savedBusiness ? (
            <>
              <VerificationStatusCard business={savedBusiness} />
              <BookkeepingResultCard prediction={savedBusiness.bookkeepingPrediction} />
            </>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle>다음 단계</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm leading-6 text-muted-foreground">
                <p>사업자 정보를 저장하면 검증 화면으로 이동해 사업자등록번호, 대표자명, 개업일자를 확인합니다.</p>
                <p>검증이 끝나야 세무자료 업로드를 안전하게 시작할 수 있습니다.</p>
              </CardContent>
            </Card>
          )}
        </aside>
      </div>
    </TooltipProvider>
  );
}

function VerificationStatusCard({ business }: { business: Business }) {
  const verified = business.verificationStatus === "VERIFIED";
  return (
    <Card>
      <CardHeader className="space-y-3">
        <div className="flex items-start justify-between gap-3">
          <CardTitle className="flex items-center gap-2">
            <ShieldCheck className="h-5 w-5" />
            사업자 검증
          </CardTitle>
          <VerificationBadge status={business.verificationStatus} />
        </div>
      </CardHeader>
      <CardContent className="space-y-4 text-sm leading-6">
        <p className="text-muted-foreground">
          {business.name} {business.businessRegistrationNumber ? `(${business.businessRegistrationNumber})` : ""}
        </p>
        <Alert variant={verified ? "success" : "warning"}>
          <AlertTitle>{verified ? "검증이 완료되었습니다" : "검증이 아직 필요합니다"}</AlertTitle>
          <AlertDescription>
            {verified
              ? "이 사업자는 파일 업로드를 진행할 수 있습니다."
              : "업로드 전 사업자등록번호, 대표자명, 개업일자를 기준으로 검증해주세요."}
          </AlertDescription>
        </Alert>
        {!verified ? (
          <Button asChild className="w-full" variant="outline">
            <Link href={`/onboarding/business/verify?businessId=${business.id}`}>
              검증하러 가기
              <ArrowRight className="h-4 w-4" />
            </Link>
          </Button>
        ) : null}
      </CardContent>
    </Card>
  );
}

export function VerificationBadge({ status }: { status: BusinessVerificationStatus }) {
  if (status === "VERIFIED") {
    return <Badge variant="success">검증 완료</Badge>;
  }
  if (status === "FAILED") {
    return <Badge variant="destructive">검증 실패</Badge>;
  }
  if (status === "NEEDS_REVIEW") {
    return <Badge variant="warning">검토 필요</Badge>;
  }
  if (status === "PENDING") {
    return <Badge variant="warning">검증 중</Badge>;
  }
  return <Badge variant="secondary">미검증</Badge>;
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
