import { PageTitle } from "@/components/app/page-title";
import { EmptyState } from "@/components/common";
import { Badge } from "@/components/ui/badge";

export function PlaceholderPage({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <section className="space-y-6">
      <PageTitle actions={<Badge variant="secondary">준비 중</Badge>} title={title} description={description} />
      <EmptyState
        title="곧 사용할 수 있습니다"
        description="현재 단계에서는 주요 신고 준비 흐름을 먼저 안정화하고 있습니다. 이 화면은 다음 개선 단계에서 연결됩니다."
      />
    </section>
  );
}
