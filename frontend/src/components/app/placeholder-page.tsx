import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function PlaceholderPage({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <section className="space-y-4">
      <div className="space-y-2">
        <Badge variant="secondary">초기 화면</Badge>
        <h1 className="text-2xl font-semibold tracking-normal">{title}</h1>
        <p className="max-w-2xl text-sm leading-6 text-muted-foreground">{description}</p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>구현 예정</CardTitle>
        </CardHeader>
        <CardContent className="text-sm leading-6 text-muted-foreground">
          현재 단계에서는 실행 가능한 프로젝트 뼈대만 구성했습니다. 다음 단계에서 도메인 API와 화면
          상호작용을 연결합니다.
        </CardContent>
      </Card>
    </section>
  );
}

