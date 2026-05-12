import { AlertCircle, CheckCircle2, HelpCircle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { BookkeepingPrediction } from "@/lib/api/businesses";

const variantByType = {
  SIMPLE_CANDIDATE: "success",
  DOUBLE_ENTRY_REQUIRED: "warning",
  NEEDS_REVIEW: "secondary",
} as const;

const iconByType = {
  SIMPLE_CANDIDATE: CheckCircle2,
  DOUBLE_ENTRY_REQUIRED: AlertCircle,
  NEEDS_REVIEW: HelpCircle,
};

export function BookkeepingResultCard({
  prediction,
}: {
  prediction: BookkeepingPrediction;
}) {
  const Icon = iconByType[prediction.bookkeepingType];

  return (
    <Card>
      <CardHeader className="space-y-3">
        <div className="flex items-center justify-between gap-3">
          <CardTitle className="flex items-center gap-2">
            <Icon className="h-5 w-5" />
            장부 유형 예측
          </CardTitle>
          <Badge variant={variantByType[prediction.bookkeepingType]}>{prediction.title}</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3 text-sm leading-6">
        <p>{prediction.message}</p>
        <p className="rounded-md bg-muted p-3 text-muted-foreground">{prediction.notice}</p>
      </CardContent>
    </Card>
  );
}
