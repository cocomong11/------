"use client";

import { useMutation } from "@tanstack/react-query";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { authApi, type OAuthProvider } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";

const providerLabel: Record<OAuthProvider, string> = {
  google: "Google",
  kakao: "카카오",
};

export function SocialAuthButtons({ mode }: { mode: "login" | "signup" }) {
  const mutation = useMutation({
    mutationFn: (provider: OAuthProvider) => {
      const redirectUri = `${window.location.origin}/login`;
      return authApi.oauthAuthorizationUrl(provider, redirectUri);
    },
    onSuccess: (response) => {
      if (response.configured && response.authorizationUrl) {
        window.location.href = response.authorizationUrl;
      }
    },
  });

  const message =
    mutation.data && !mutation.data.configured
      ? mutation.data.message
      : mutation.error instanceof ApiError
        ? mutation.error.message
        : null;

  return (
    <div className="space-y-3">
      <div className="grid gap-2 sm:grid-cols-2">
        <SocialButton
          disabled={mutation.isPending}
          label={`${providerLabel.google}로 ${mode === "login" ? "로그인" : "가입"}`}
          provider="google"
          onClick={() => mutation.mutate("google")}
        />
        <SocialButton
          disabled={mutation.isPending}
          label={`${providerLabel.kakao}로 ${mode === "login" ? "로그인" : "가입"}`}
          provider="kakao"
          onClick={() => mutation.mutate("kakao")}
        />
      </div>
      {message ? (
        <Alert variant="warning">
          <AlertTitle>소셜 로그인을 준비 중입니다</AlertTitle>
          <AlertDescription>{message}</AlertDescription>
        </Alert>
      ) : null}
    </div>
  );
}

function SocialButton({
  disabled,
  label,
  onClick,
  provider,
}: {
  disabled: boolean;
  label: string;
  onClick: () => void;
  provider: OAuthProvider;
}) {
  return (
    <Button
      className={provider === "kakao" ? "bg-[#FEE500] text-[#191919] hover:bg-[#F4DA00]" : ""}
      disabled={disabled}
      onClick={onClick}
      type="button"
      variant={provider === "google" ? "outline" : "default"}
    >
      <span className="inline-flex h-5 w-5 items-center justify-center rounded-sm bg-background text-xs font-bold text-foreground">
        {provider === "google" ? "G" : "K"}
      </span>
      {label}
    </Button>
  );
}
