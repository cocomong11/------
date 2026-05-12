"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { CheckCircle2, FileText, LockKeyhole, MailCheck, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { SocialAuthButtons } from "@/components/auth/social-auth-buttons";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Form, FormDescription, FormItem, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { authApi } from "@/lib/api/auth";
import { ApiError, setAuthTokens } from "@/lib/api/client";

const requiredAgreement = (message: string) => z.boolean().refine(Boolean, message);

const signupSchema = z
  .object({
    name: z.string().min(2, "이름을 2자 이상 입력해주세요.").max(100),
    email: z.string().email("이메일 형식을 확인해주세요."),
    password: z
      .string()
      .min(8, "비밀번호는 8자 이상이어야 합니다.")
      .max(100)
      .regex(/[A-Za-z]/, "영문을 포함해주세요.")
      .regex(/[0-9]/, "숫자를 포함해주세요."),
    passwordConfirm: z.string().min(1, "비밀번호를 한 번 더 입력해주세요."),
    termsAgreed: requiredAgreement("서비스 이용약관에 동의해주세요."),
    privacyAgreed: requiredAgreement("개인정보 처리방침에 동의해주세요."),
    businessInfoConsentAgreed: requiredAgreement("사업자정보 수집 및 이용에 동의해주세요."),
    taxDataConsentAgreed: requiredAgreement("세무자료 업로드 및 처리에 동의해주세요."),
    referenceNoticeAgreed: requiredAgreement("결과가 참고용이라는 안내를 확인해주세요."),
    marketingAgreed: z.boolean().default(false),
  })
  .refine((values) => values.password === values.passwordConfirm, {
    message: "비밀번호가 서로 다릅니다.",
    path: ["passwordConfirm"],
  });

type SignupForm = z.infer<typeof signupSchema>;

export default function SignupPage() {
  const router = useRouter();
  const form = useForm<SignupForm>({
    resolver: zodResolver(signupSchema),
    defaultValues: {
      name: "",
      email: "",
      password: "",
      passwordConfirm: "",
      termsAgreed: false,
      privacyAgreed: false,
      businessInfoConsentAgreed: false,
      taxDataConsentAgreed: false,
      referenceNoticeAgreed: false,
      marketingAgreed: false,
    },
  });

  const password = form.watch("password");
  const strength = passwordStrength(password);

  const mutation = useMutation({
    mutationFn: authApi.signup,
    onSuccess: (response, request) => {
      window.sessionStorage.setItem("pendingVerificationEmail", request.email);
      if (response.accessToken) {
        setAuthTokens(response.accessToken, response.refreshToken, true);
        router.push("/onboarding/business");
        return;
      }
      router.push(`/verify-email?email=${encodeURIComponent(request.email)}`);
    },
  });

  function onSubmit(values: SignupForm) {
    mutation.mutate({
      name: values.name,
      email: values.email,
      password: values.password,
      termsAgreed: values.termsAgreed,
      privacyAgreed: values.privacyAgreed,
      businessInfoConsentAgreed: values.businessInfoConsentAgreed,
      taxDataConsentAgreed: values.taxDataConsentAgreed,
      referenceNoticeAgreed: values.referenceNoticeAgreed,
    });
  }

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.error
        ? "회원가입 중 오류가 발생했습니다."
        : null;

  return (
    <section className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
      <Card>
        <CardHeader className="space-y-3">
          <div className="flex items-center gap-2 text-sm font-medium text-primary">
            <ShieldCheck className="h-4 w-4" />
            세무자료를 다루는 계정 보호 절차
          </div>
          <CardTitle className="text-2xl">회원가입</CardTitle>
          <p className="text-sm leading-6 text-muted-foreground">
            이메일 인증과 필수 동의 확인 후 사업자 정보를 등록할 수 있습니다. 공동인증서 저장이나 홈택스 자동 로그인은
            진행하지 않습니다.
          </p>
        </CardHeader>
        <CardContent>
          <SocialAuthButtons mode="signup" />
          <div className="my-5 flex items-center gap-3 text-xs text-muted-foreground">
            <span className="h-px flex-1 bg-border" />
            이메일로 가입
            <span className="h-px flex-1 bg-border" />
          </div>
          <Form {...form}>
            <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)}>
              <div className="grid gap-4 sm:grid-cols-2">
                <FormItem>
                  <Label htmlFor="name">이름</Label>
                  <Input id="name" autoComplete="name" {...form.register("name")} />
                  {form.formState.errors.name ? <FormMessage>{form.formState.errors.name.message}</FormMessage> : null}
                </FormItem>
                <FormItem>
                  <Label htmlFor="email">이메일</Label>
                  <Input id="email" type="email" autoComplete="email" {...form.register("email")} />
                  {form.formState.errors.email ? (
                    <FormMessage>{form.formState.errors.email.message}</FormMessage>
                  ) : null}
                </FormItem>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <FormItem>
                  <Label htmlFor="password">비밀번호</Label>
                  <Input id="password" type="password" autoComplete="new-password" {...form.register("password")} />
                  <div className="space-y-2">
                    <Progress aria-label="비밀번호 강도" value={strength.score} />
                    <FormDescription>{strength.label}</FormDescription>
                  </div>
                  {form.formState.errors.password ? (
                    <FormMessage>{form.formState.errors.password.message}</FormMessage>
                  ) : null}
                </FormItem>
                <FormItem>
                  <Label htmlFor="passwordConfirm">비밀번호 확인</Label>
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
              </div>

              <div className="rounded-md border p-4">
                <div className="mb-3 flex items-center justify-between gap-3">
                  <div>
                    <h2 className="font-semibold tracking-normal">약관 및 자료 처리 동의</h2>
                    <p className="mt-1 text-xs leading-5 text-muted-foreground">필수 항목은 서비스 이용을 위해 필요합니다.</p>
                  </div>
                  <TermsDialog />
                </div>
                <div className="grid gap-3">
                  <AgreementCheckbox name="termsAgreed" label="[필수] 서비스 이용약관" form={form} />
                  <AgreementCheckbox name="privacyAgreed" label="[필수] 개인정보 처리방침" form={form} />
                  <AgreementCheckbox name="businessInfoConsentAgreed" label="[필수] 사업자정보 수집 및 이용 동의" form={form} />
                  <AgreementCheckbox name="taxDataConsentAgreed" label="[필수] 세무자료 업로드 및 처리 동의" form={form} />
                  <AgreementCheckbox name="referenceNoticeAgreed" label="[필수] 결과는 참고용이며 신고 책임은 사용자에게 있음을 확인" form={form} />
                  <AgreementCheckbox name="marketingAgreed" label="[선택] 서비스 개선 안내 수신" form={form} />
                </div>
              </div>

              {errorMessage ? (
                <Alert variant="destructive">
                  <AlertTitle>가입을 완료하지 못했습니다</AlertTitle>
                  <AlertDescription>{errorMessage}</AlertDescription>
                </Alert>
              ) : null}

              <Button className="w-full" disabled={mutation.isPending} type="submit">
                {mutation.isPending ? "가입 처리 중" : "이메일 인증 시작"}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                이미 계정이 있나요?{" "}
                <Link className="font-medium text-primary underline-offset-4 hover:underline" href="/login">
                  로그인
                </Link>
              </p>
            </form>
          </Form>
        </CardContent>
      </Card>

      <div className="space-y-4">
        <TrustStep icon={MailCheck} title="1. 이메일 인증" description="6자리 코드로 본인 이메일을 확인합니다." />
        <TrustStep icon={FileText} title="2. 약관 동의 보관" description="세무자료 처리 동의 이력을 안전하게 남깁니다." />
        <TrustStep icon={LockKeyhole} title="3. 토큰 보호" description="짧은 로그인 토큰과 재발급 토큰으로 접근을 분리합니다." />
        <Alert>
          <AlertTitle>안심 안내</AlertTitle>
          <AlertDescription>
            사업자등록번호는 화면과 로그에서 전체값을 노출하지 않으며, 업로드는 인증과 사업자 검증 후 진행됩니다.
          </AlertDescription>
        </Alert>
      </div>
    </section>
  );
}

function AgreementCheckbox({
  form,
  label,
  name,
}: {
  form: ReturnType<typeof useForm<SignupForm>>;
  label: string;
  name: keyof SignupForm;
}) {
  const message = form.formState.errors[name]?.message;
  return (
    <label className="flex items-start gap-3 text-sm leading-6">
      <input className="mt-1 h-4 w-4 shrink-0" type="checkbox" {...form.register(name)} />
      <span>
        {label}
        {message ? <span className="block text-destructive">{String(message)}</span> : null}
      </span>
    </label>
  );
}

function TermsDialog() {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button size="sm" type="button" variant="outline">
          상세 보기
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>필수 동의 항목 안내</DialogTitle>
          <DialogDescription>
            실제 약관 전문은 추후 버전 관리 화면으로 확장할 수 있도록 서버에 동의 버전을 저장합니다.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3 text-sm leading-6 text-muted-foreground">
          <p>서비스 이용약관, 개인정보 처리방침, 사업자정보 수집 및 이용 동의가 필요합니다.</p>
          <p>세무자료 업로드 및 처리 동의는 거래 파일 분석과 리포트 생성을 위해 필요합니다.</p>
          <p>자동 계산 결과는 참고용이며 최종 신고와 기장의무 판단은 사용자 확인이 필요합니다.</p>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function TrustStep({
  description,
  icon: Icon,
  title,
}: {
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  title: string;
}) {
  return (
    <Card>
      <CardContent className="flex gap-3 pt-6">
        <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-secondary">
          <Icon className="h-5 w-5" />
        </span>
        <div>
          <div className="flex items-center gap-2 font-semibold">
            {title}
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </div>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">{description}</p>
        </div>
      </CardContent>
    </Card>
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
