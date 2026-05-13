import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

export function LoadingState({
  className,
  label = "불러오는 중입니다.",
}: {
  className?: string;
  label?: string;
}) {
  return (
    <div className={cn("flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-3 text-[13px] text-muted-foreground", className)}>
      <Loader2 className="h-4 w-4 animate-spin" />
      {label}
    </div>
  );
}
