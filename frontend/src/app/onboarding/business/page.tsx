import { BusinessForm } from "@/components/business/business-form";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export default function BusinessOnboardingPage() {
  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">사업자 온보딩</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          사업자 검증과 세무자료 업로드 전에 필요한 기본 정보를 등록합니다.
        </p>
      </div>
      <Alert>
        <AlertTitle>공동인증서나 홈택스 비밀번호를 요청하지 않습니다</AlertTitle>
        <AlertDescription>
          이 단계는 사용자가 직접 입력한 사업자 정보 기준 확인입니다. 홈택스 로그인 자동화나 공동인증서 저장은 제공하지 않습니다.
        </AlertDescription>
      </Alert>
      <BusinessForm />
    </section>
  );
}
