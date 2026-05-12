"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, HelpCircle, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { VerificationBadge } from "@/components/business/business-form";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormDescription, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { businessesApi, type BusinessVerificationResponse } from "@/lib/api/businesses";
import { ApiError } from "@/lib/api/client";

const verificationSchema = z.object({
  businessRegistrationNumber: z
    .string()
    .transform((value) => value.replace(/\D/g, ""))
    .refine((value) => value.length === 10, "사업자등록번호 숫자 10자리를 입력해주세요."),
  representativeName: z.string().min(2, "대표자명을 입력해주세요.").max(100),
  openedOn: z.string().min(1, "개업일자를 선택해주세요."),
});

type VerificationForm = z.infer<typeof verificationSchema>;

export default function BusinessVerifyPage() {
  return (
    <Suspense fallback={null}>
      <BusinessVerifyContent />
    </Suspense>
  );
}

function BusinessVerifyContent() {
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const [selectedBusinessId, setSelectedBusinessId] = useState(searchParams.get("businessId") ?? "");
  const [result, setResult] = useState<BusinessVerificationResponse | null>(null);

  const businessesQuery = useQuery({
    queryKey: ["businesses"],
    queryFn: businessesApi.list,
  });
  const businesses = useMemo(() => businessesQuery.data ?? [], [businessesQuery.data]);
  const activeBusinessId = selectedBusinessId || businesses[0]?.id || "";
  const activeBusiness = useMemo(
    () => businesses.find((business) => business.id === activeBusinessId),
    [activeBusinessId, businesses],
  );

  const form = useForm<VerificationForm>({
    resolver: zodResolver(verificationSchema),
    defaultValues: {
      businessRegistrationNumber: "",
      representativeName: "",
      openedOn: "",
    },
  });

  useEffect(() => {
    if (!selectedBusinessId && businesses[0]?.id) {
      setSelectedBusinessId(businesses[0].id);
    }
  }, [businesses, selectedBusinessId]);

  useEffect(() => {
    if (!activeBusiness) {
      return;
    }
    const masked = activeBusiness.businessRegistrationNumber?.includes("*");
    form.reset({
      businessRegistrationNumber: masked ? "" : activeBusiness.businessRegistrationNumber?.replace(/\D/g, "") ?? "",
      representativeName: activeBusiness.representativeName ?? "",
      openedOn: activeBusiness.openedOn ?? "",
    });
    setResult(null);
  }, [activeBusiness, form]);

  const mutation = useMutation({
    mutationFn: (values: VerificationForm) => {
      if (!activeBusinessId) {
        throw new Error("검증할 사업자를 선택해주세요.");
      }
      return businessesApi.verify(activeBusinessId, values);
    },
    onSuccess: (response) => {
      setResult(response);
      void queryClient.invalidateQueries({ queryKey: ["businesses"] });
    },
  });

  function onSubmit(values: VerificationForm) {
    mutation.mutate(values);
  }

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.error instanceof Error
        ? mutation.error.message
        : businessesQuery.error instanceof ApiError
          ? businessesQuery.error.message
          : null;

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">사업자 검증</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          사업자등록번호, 대표자명, 개업일자를 기준으로 검증합니다. 현재는 외부 API 교체를 고려한 Mock 검증입니다.
        </p>
      </div>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_420px]">
        <Card>
          <CardHeader className="space-y-3">
            <CardTitle className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5" />
              검증 정보 입력
            </CardTitle>
          </CardHeader>
          <CardContent>
            {businesses.length === 0 && !businessesQuery.isLoading ? (
              <EmptyBusinessNotice />
            ) : (
              <Form {...form}>
                <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)}>
                  <FormItem>
                    <Label htmlFor="businessId">사업자</Label>
                    <select
                      id="businessId"
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      disabled={businessesQuery.isLoading || businesses.length === 0}
                      value={activeBusinessId}
                      onChange={(event) => setSelectedBusinessId(event.target.value)}
                    >
                      {businesses.map((business) => (
                        <option key={business.id} value={business.id}>
                          {business.name} · {business.businessRegistrationNumber ?? "번호 미입력"}
                        </option>
                      ))}
                    </select>
                    {activeBusiness ? (
                      <FormDescription>
                        현재 상태: <VerificationBadge status={activeBusiness.verificationStatus} />
                      </FormDescription>
                    ) : null}
                  </FormItem>

                  <div className="grid gap-4 md:grid-cols-2">
                    <FormItem>
                      <Label htmlFor="businessRegistrationNumber">사업자등록번호</Label>
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
                  </div>

                  <Alert>
                    <AlertTitle>검증 기준 안내</AlertTitle>
                    <AlertDescription>
                      입력값 기준 확인 결과이며, 최종 기장의무와 신고 책임은 사용자에게 있습니다.
                    </AlertDescription>
                  </Alert>

                  {errorMessage ? (
                    <Alert variant="destructive">
                      <AlertTitle>검증할 수 없습니다</AlertTitle>
                      <AlertDescription>{errorMessage}</AlertDescription>
                    </Alert>
                  ) : null}

                  <Button disabled={!activeBusinessId || mutation.isPending} type="submit">
                    {mutation.isPending ? "검증 중" : "사업자 검증"}
                  </Button>
                </form>
              </Form>
            )}
          </CardContent>
        </Card>

        <VerificationResultCard result={result} />
      </div>
    </section>
  );
}

function VerificationResultCard({ result }: { result: BusinessVerificationResponse | null }) {
  if (!result) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>검증 결과</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm leading-6 text-muted-foreground">
          <p>검증을 실행하면 완료, 실패, 검토 필요 상태가 이곳에 표시됩니다.</p>
          <p>외부 공공 API 연동 전까지는 Mock 검증 provider가 같은 인터페이스로 동작합니다.</p>
        </CardContent>
      </Card>
    );
  }

  const state = result.verificationStatus;
  const Icon = state === "VERIFIED" ? CheckCircle2 : state === "FAILED" ? AlertTriangle : HelpCircle;

  return (
    <Card>
      <CardHeader className="space-y-3">
        <div className="flex items-start justify-between gap-3">
          <CardTitle className="flex items-center gap-2">
            <Icon className="h-5 w-5" />
            {result.title}
          </CardTitle>
          <VerificationBadge status={state} />
        </div>
      </CardHeader>
      <CardContent className="space-y-4 text-sm leading-6">
        <Alert variant={state === "VERIFIED" ? "success" : state === "FAILED" ? "destructive" : "warning"}>
          <AlertTitle>
            {state === "VERIFIED" ? "검증 완료" : state === "FAILED" ? "검증 실패" : "검토 필요"}
          </AlertTitle>
          <AlertDescription>{result.message}</AlertDescription>
        </Alert>
        <p className="rounded-md bg-muted p-3 text-muted-foreground">{result.notice}</p>
        <div className="flex flex-wrap gap-2">
          <Badge variant="secondary">입력값 기준</Badge>
          <Badge variant="secondary">최종 신고 책임 사용자</Badge>
        </div>
        {state === "VERIFIED" ? (
          <Button asChild>
            <Link href="/files">세무자료 업로드로 이동</Link>
          </Button>
        ) : null}
      </CardContent>
    </Card>
  );
}

function EmptyBusinessNotice() {
  return (
    <Alert variant="warning">
      <AlertTitle>등록된 사업자가 없습니다</AlertTitle>
      <AlertDescription>
        사업자 정보를 먼저 등록한 뒤 검증을 진행해주세요.{" "}
        <Link className="font-medium underline underline-offset-4" href="/onboarding/business">
          등록 화면으로 이동
        </Link>
      </AlertDescription>
    </Alert>
  );
}
