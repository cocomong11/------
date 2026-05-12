"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { MailQuestion } from "lucide-react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormDescription, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authApi } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";

const forgotPasswordSchema = z.object({
  email: z.string().email("이메일 형식을 확인해주세요."),
});

type ForgotPasswordForm = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordPage() {
  const form = useForm<ForgotPasswordForm>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: "" },
  });

  const mutation = useMutation({
    mutationFn: authApi.forgotPassword,
  });

  function onSubmit(values: ForgotPasswordForm) {
    mutation.mutate(values.email);
  }

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.error
        ? "비밀번호 재설정 요청 중 오류가 발생했습니다."
        : null;

  return (
    <section className="mx-auto max-w-md">
      <Card>
        <CardHeader className="space-y-3">
          <div className="flex items-center gap-2 text-sm font-medium text-primary">
            <MailQuestion className="h-4 w-4" />
            계정 복구
          </div>
          <CardTitle className="text-2xl">비밀번호 찾기</CardTitle>
          <p className="text-sm leading-6 text-muted-foreground">
            가입 이메일을 입력하면 재설정 토큰을 발급합니다. 개발 환경에서는 실제 메일 대신 DB에 저장됩니다.
          </p>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)}>
              <FormItem>
                <Label htmlFor="email">이메일</Label>
                <Input id="email" type="email" autoComplete="email" {...form.register("email")} />
                <FormDescription>보안을 위해 가입 여부와 관계없이 같은 안내가 표시될 수 있습니다.</FormDescription>
                {form.formState.errors.email ? <FormMessage>{form.formState.errors.email.message}</FormMessage> : null}
              </FormItem>

              {mutation.isSuccess ? (
                <Alert variant="success">
                  <AlertTitle>재설정 요청을 접수했습니다</AlertTitle>
                  <AlertDescription>재설정 토큰은 제한된 시간 동안만 사용할 수 있습니다.</AlertDescription>
                </Alert>
              ) : null}

              {errorMessage ? (
                <Alert variant="destructive">
                  <AlertTitle>요청을 처리하지 못했습니다</AlertTitle>
                  <AlertDescription>{errorMessage}</AlertDescription>
                </Alert>
              ) : null}

              <Button className="w-full" disabled={mutation.isPending} type="submit">
                {mutation.isPending ? "요청 중" : "재설정 요청"}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                기억나셨나요?{" "}
                <Link className="font-medium text-primary underline-offset-4 hover:underline" href="/login">
                  로그인
                </Link>
              </p>
            </form>
          </Form>
        </CardContent>
      </Card>
    </section>
  );
}
