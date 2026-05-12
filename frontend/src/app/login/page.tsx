"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { KeyRound, MailCheck, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { SocialAuthButtons } from "@/components/auth/social-auth-buttons";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authApi } from "@/lib/api/auth";
import { ApiError, setAuthTokens } from "@/lib/api/client";

const loginSchema = z.object({
  email: z.string().email("이메일 형식을 확인해주세요."),
  password: z.string().min(8, "비밀번호는 8자 이상이어야 합니다."),
  rememberMe: z.boolean().default(true),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const form = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
      rememberMe: true,
    },
  });

  const resendMutation = useMutation({
    mutationFn: authApi.resendVerificationCode,
  });

  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (response, request) => {
      if (!response.accessToken) {
        window.sessionStorage.setItem("pendingVerificationEmail", request.email);
        router.push(`/verify-email?email=${encodeURIComponent(request.email)}`);
        return;
      }
      setAuthTokens(response.accessToken, response.refreshToken, form.getValues("rememberMe"));
      router.push("/dashboard");
    },
  });

  function onSubmit(values: LoginForm) {
    mutation.mutate({ email: values.email, password: values.password });
  }

  function resendVerification() {
    const email = form.getValues("email");
    if (!email) {
      form.setError("email", { message: "인증 코드를 받을 이메일을 입력해주세요." });
      return;
    }
    window.sessionStorage.setItem("pendingVerificationEmail", email);
    resendMutation.mutate(email);
  }

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.error
        ? "로그인 중 오류가 발생했습니다."
        : null;
  const isEmailVerificationError = mutation.error instanceof ApiError && mutation.error.code === "EMAIL_NOT_VERIFIED";

  return (
    <section className="mx-auto grid max-w-4xl gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
      <Card>
        <CardHeader className="space-y-3">
          <div className="flex items-center gap-2 text-sm font-medium text-primary">
            <ShieldCheck className="h-4 w-4" />
            안전한 세무 서비스 로그인
          </div>
          <CardTitle className="text-2xl">로그인</CardTitle>
          <p className="text-sm leading-6 text-muted-foreground">
            이메일 인증이 끝난 계정만 대시보드와 자료 업로드 기능을 사용할 수 있습니다.
          </p>
        </CardHeader>
        <CardContent>
          <SocialAuthButtons mode="login" />
          <div className="my-5 flex items-center gap-3 text-xs text-muted-foreground">
            <span className="h-px flex-1 bg-border" />
            이메일로 로그인
            <span className="h-px flex-1 bg-border" />
          </div>
          <Form {...form}>
            <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)}>
              <FormItem>
                <Label htmlFor="email">이메일</Label>
                <Input id="email" type="email" autoComplete="email" {...form.register("email")} />
                {form.formState.errors.email ? <FormMessage>{form.formState.errors.email.message}</FormMessage> : null}
              </FormItem>
              <FormItem>
                <div className="flex items-center justify-between gap-3">
                  <Label htmlFor="password">비밀번호</Label>
                  <Link className="text-sm font-medium text-primary underline-offset-4 hover:underline" href="/forgot-password">
                    비밀번호 찾기
                  </Link>
                </div>
                <Input id="password" type="password" autoComplete="current-password" {...form.register("password")} />
                {form.formState.errors.password ? (
                  <FormMessage>{form.formState.errors.password.message}</FormMessage>
                ) : null}
              </FormItem>

              <label className="flex items-center gap-2 text-sm">
                <input className="h-4 w-4" type="checkbox" {...form.register("rememberMe")} />
                로그인 유지
              </label>

              {errorMessage ? (
                <Alert variant={isEmailVerificationError ? "warning" : "destructive"}>
                  <AlertTitle>{isEmailVerificationError ? "이메일 인증이 필요합니다" : "로그인할 수 없습니다"}</AlertTitle>
                  <AlertDescription>{errorMessage}</AlertDescription>
                </Alert>
              ) : null}

              {resendMutation.isSuccess ? (
                <Alert variant="success">
                  <AlertTitle>인증 코드를 다시 보냈습니다</AlertTitle>
                  <AlertDescription>개발 환경에서는 DB에 저장된 최신 코드를 확인해 인증할 수 있습니다.</AlertDescription>
                </Alert>
              ) : null}

              <div className="grid gap-3 sm:grid-cols-2">
                <Button disabled={mutation.isPending} type="submit">
                  {mutation.isPending ? "로그인 중" : "로그인"}
                </Button>
                <Button disabled={resendMutation.isPending} onClick={resendVerification} type="button" variant="outline">
                  이메일 인증 재전송
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      <div className="space-y-4">
        <Card>
          <CardContent className="flex gap-3 pt-6">
            <MailCheck className="mt-1 h-5 w-5 shrink-0 text-primary" />
            <div>
              <h2 className="font-semibold tracking-normal">인증 코드가 먼저입니다</h2>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                가입 후 10분 안에 6자리 코드를 입력해야 계정이 활성화됩니다.
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex gap-3 pt-6">
            <KeyRound className="mt-1 h-5 w-5 shrink-0 text-primary" />
            <div>
              <h2 className="font-semibold tracking-normal">반복 실패 시 잠금</h2>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                비밀번호가 5회 이상 틀리면 일정 시간 로그인이 제한됩니다.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </section>
  );
}
