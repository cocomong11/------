"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { KeyRound } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormDescription, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";

const resetPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, "비밀번호는 8자 이상이어야 합니다.")
      .max(100)
      .regex(/[A-Za-z]/, "영문을 포함해주세요.")
      .regex(/[0-9]/, "숫자를 포함해주세요."),
    passwordConfirm: z.string().min(1, "비밀번호를 한 번 더 입력해주세요."),
  })
  .refine((values) => values.newPassword === values.passwordConfirm, {
    message: "비밀번호가 서로 다릅니다.",
    path: ["passwordConfirm"],
  });

type ResetPasswordForm = z.infer<typeof resetPasswordSchema>;

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={null}>
      <ResetPasswordContent />
    </Suspense>
  );
}

function ResetPasswordContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const form = useForm<ResetPasswordForm>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: "", passwordConfirm: "" },
  });
  const strength = passwordStrength(form.watch("newPassword"));

  const mutation = useMutation({
    mutationFn: authApi.resetPassword,
  });

  function onSubmit(values: ResetPasswordForm) {
    if (!token) {
      form.setError("newPassword", { message: "재설정 토큰이 없습니다. 이메일의 링크로 다시 접속해주세요." });
      return;
    }
    mutation.mutate({ token, newPassword: values.newPassword });
  }

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.error
        ? "비밀번호 재설정 중 오류가 발생했습니다."
        : null;

  return (
    <section className="mx-auto max-w-md">
      <Card>
        <CardHeader className="space-y-3">
          <div className="flex items-center gap-2 text-sm font-medium text-primary">
            <KeyRound className="h-4 w-4" />
            새 비밀번호 설정
          </div>
          <CardTitle className="text-2xl">비밀번호 재설정</CardTitle>
          <p className="text-sm leading-6 text-muted-foreground">
            재설정 토큰은 일정 시간 후 만료됩니다. 기존 비밀번호와 다른 안전한 비밀번호를 사용해주세요.
          </p>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)}>
              <FormItem>
                <Label htmlFor="newPassword">새 비밀번호</Label>
                <Input id="newPassword" type="password" autoComplete="new-password" {...form.register("newPassword")} />
                <Progress aria-label="비밀번호 강도" value={strength.score} />
                <FormDescription>{strength.label}</FormDescription>
                {form.formState.errors.newPassword ? (
                  <FormMessage>{form.formState.errors.newPassword.message}</FormMessage>
                ) : null}
              </FormItem>
              <FormItem>
                <Label htmlFor="passwordConfirm">새 비밀번호 확인</Label>
                <Input
                  id="passwordConfirm"
                  type="password"
                  autoComplete="new-password"
                  {...form.register("passwordConfirm")}
                />
                {form.formState.errors.passwordConfirm ? (
                  <FormMessage>{form.formState.errors.passwordConfirm.message}</FormMessage>
                ) : null}
              </FormItem>

              {mutation.isSuccess ? (
                <Alert variant="success">
                  <AlertTitle>비밀번호를 변경했습니다</AlertTitle>
                  <AlertDescription>새 비밀번호로 다시 로그인해주세요.</AlertDescription>
                </Alert>
              ) : null}

              {errorMessage ? (
                <Alert variant="destructive">
                  <AlertTitle>변경할 수 없습니다</AlertTitle>
                  <AlertDescription>{errorMessage}</AlertDescription>
                </Alert>
              ) : null}

              <Button className="w-full" disabled={mutation.isPending || mutation.isSuccess} type="submit">
                {mutation.isPending ? "변경 중" : "비밀번호 변경"}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                <Link className="font-medium text-primary underline-offset-4 hover:underline" href="/login">
                  로그인으로 이동
                </Link>
              </p>
            </form>
          </Form>
        </CardContent>
      </Card>
    </section>
  );
}

function passwordStrength(password: string) {
  let score = 0;
  if (password.length >= 8) score += 25;
  if (/[A-Za-z]/.test(password)) score += 25;
  if (/[0-9]/.test(password)) score += 25;
  if (/[^A-Za-z0-9]/.test(password)) score += 25;

  if (score >= 100) {
    return { score, label: "강함: 영문, 숫자, 특수문자가 포함되어 있습니다." };
  }
  if (score >= 75) {
    return { score, label: "보통: 특수문자를 더하면 더 안전합니다." };
  }
  if (score >= 50) {
    return { score, label: "약함: 영문과 숫자를 함께 사용해주세요." };
  }
  return { score, label: "8자 이상, 영문과 숫자를 포함해주세요." };
}
