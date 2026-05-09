import { BusinessForm } from "@/components/business/business-form";

export default function OnboardingPage() {
  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">사업자 등록</h1>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          업종 그룹과 직전연도 수입금액을 입력하면 간편장부 대상 여부를 입력값 기준으로 예상합니다.
        </p>
      </div>
      <BusinessForm />
    </section>
  );
}
