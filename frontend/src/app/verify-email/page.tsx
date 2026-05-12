"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { MailCheck, RotateCcw, ShieldCheck } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormDescription, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authApi } from "@/lib/api/auth";
import { ApiError, setAuthTokens } from "@/lib/api/client";

const verifySchema = z.object({
  email: z.string().email("이메일 형식을 확인해주세요."),
  code: z.string().regex(/^\d{6}$/, "6자리 숫자 코드를 입력해주세요."),
});

type VerifyForm = z.infer<typeof verifySchema>;

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={null}>
      <VerifyEmailContent />
    </Suspense>
  );
}

function VerifyEmailContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [remainingSeconds, setRemainingSeconds] = useState(600);
  const form = useForm<VerifyForm>({
    resolver: zodResolver(verifySchema),
    defaultValues: {
      email: "",
      code: "",
    },
  });

  useEffect(() => {
    const email = searchParams.get("email") ?? window.sessionStorage.getItem("pendingVerificationEmail") ?? "";
    form.setValue("email", email);
  }, [form, searchParams]);

  useEffect(() => {
    if (remainingSeconds <= 0) {
      return;
    }
    const timer = window.setInterval(() => {
      setRemainingSeconds((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [remainingSeconds]);

  const verifyMutation = useMutation({
    mutationFn: authApi.verifyEmail,
    onSuccess: (response) => {
      if (response.accessToken) {
        setAuthTokens(response.accessToken, response.refreshToken, true);
      }
      router.push("/onboarding/business");
    },
  });

  const resendMutation = useMutation({
    mutationFn: authApi.resendVerificationCode,
    onSuccess: () => {
      setRemainingSeconds(600);
      form.setValue("code", "");
    },
  });

  function onSubmit(values: VerifyForm) {
    verifyMutation.mutate(values);
  }

  function resendCode() {
    const email = form.getValues("email");
    if (!email) {
      form.setError("email", { message: "인증 코드를 받을 이메일을 입력해주세요." });
      return;
    }
    resendMutation.mutate(email);
  }

  const errorMessage =
    verifyMutation.error instanceof ApiError
      ? verifyMutation.error.message
      : verifyMutation.error
        ? "이메일 인증 중 오류가 발생했습니다."
        : null;

  return (
    <section className="mx-auto grid max-w-4xl gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
      <Card>
        <CardHeader className="space-y-3">
          <div className="flex items-center gap-2 text-sm font-medium text-primary">
            <MailCheck className="h-4 w-4" />
            이메일 본인 확인
          </div>
          <CardTitle className="text-2xl">인증 코드 입력</CardTitle>
          <p className="text-sm leading-6 text-muted-foreground">
            가입한 이메일로 발급된 6자리 코드를 입력해주세요. 코드는 발급 후 10분 동안 유효합니다.
          </p>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)}>
              <FormItem>
                <Label htmlFor="email">이메일</Label>
                <Input id="email" type="email" autoComplete="email" {...form.register("email")} />
                {form.formState.errors.email ? <FormMessage>{form.formState.errors.email.message}</FormMessage> : null}
              </FormItem>
              <FormItem>
                <div className="flex items-center justify-between gap-3">
                  <Label htmlFor="code">6자리 인증 코드</Label>
                  <span className="text-sm font-medium text-primary">{formatRemainingTime(remainingSeconds)}</span>
                </div>
                <Input
                  id="code"
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="123456"
                  {...form.register("code")}
                />
                <FormDescription>개발 환경에서는 메일 발송 대신 DB에 저장된 최신 코드를 사용할 수 있습니다.</FormDescription>
                {form.formState.errors.code ? <FormMessage>{form.formState.errors.code.message}</FormMessage> : null}
              </FormItem>

              {errorMessage ? (
                <Alert variant="destructive">
                  <AlertTitle>인증할 수 없습니다</AlertTitle>
                  <AlertDescription>{errorMessage}</AlertDescription>
                </Alert>
              ) : null}

              {resendMutation.isSuccess ? (
                <Alert variant="success">
                  <AlertTitle>새 인증 코드를 준비했습니다</AlertTitle>
                  <AlertDescription>가장 최근에 발급된 코드만 사용할 수 있습니다.</AlertDescription>
                </Alert>
              ) : null}

              <div className="grid gap-3 sm:grid-cols-2">
                <Button disabled={verifyMutation.isPending} type="submit">
                  {verifyMutation.isPending ? "인증 확인 중" : "인증 완료"}
                </Button>
                <Button disabled={resendMutation.isPending} onClick={resendCode} type="button" variant="outline">
                  <RotateCcw className="h-4 w-4" />
                  코드 재전송
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex gap-3 pt-6">
          <ShieldCheck className="mt-1 h-5 w-5 shrink-0 text-primary" />
          <div>
            <h2 className="font-semibold tracking-normal">인증 전 제한</h2>
            <p className="mt-1 text-sm leading-6 text-muted-foreground">
              이메일 인증 전에는 대시보드, 사업자 등록, 파일 업로드 같은 주요 기능이 제한됩니다.
            </p>
          </div>
        </CardContent>
      </Card>
    </section>
  );
}

function formatRemainingTime(seconds: number) {
  const minutes = Math.floor(seconds / 60)
    .toString()
    .padStart(2, "0");
  const rest = (seconds % 60).toString().padStart(2, "0");
  return `${minutes}:${rest}`;
}
