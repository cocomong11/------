import { AlertTriangle, CheckCircle2, Info, ShieldAlert } from "lucide-react";
import { Badge } from "@/components/ui/badge";

export type StatusTone = "success" | "warning" | "danger" | "info" | "neutral";

export function StatusBadge({
  children,
  tone = "neutral",
}: {
  children: React.ReactNode;
  tone?: StatusTone;
}) {
  if (tone === "success") {
    return (
      <Badge variant="success">
        <CheckCircle2 className="mr-1 h-3 w-3" />
        {children}
      </Badge>
    );
  }
  if (tone === "warning") {
    return (
      <Badge variant="warning">
        <AlertTriangle className="mr-1 h-3 w-3" />
        {children}
      </Badge>
    );
  }
  if (tone === "danger") {
    return (
      <Badge variant="destructive">
        <ShieldAlert className="mr-1 h-3 w-3" />
        {children}
      </Badge>
    );
  }
  if (tone === "info") {
    return (
      <Badge variant="info">
        <Info className="mr-1 h-3 w-3" />
        {children}
      </Badge>
    );
  }
  return <Badge variant="secondary">{children}</Badge>;
}
