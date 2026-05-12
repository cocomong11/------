import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { HelpTooltip } from "./help-tooltip";
import { StatusBadge, type StatusTone } from "./status-badge";

export function StatCard({
  className,
  help,
  icon: Icon,
  label,
  status,
  tone = "neutral",
  value,
}: {
  className?: string;
  help?: React.ReactNode;
  icon?: React.ComponentType<{ className?: string }>;
  label: string;
  status?: string;
  tone?: StatusTone;
  value: React.ReactNode;
}) {
  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardHeader className="flex flex-row items-start justify-between gap-3 space-y-0 pb-3">
        <CardTitle className="flex min-w-0 items-center gap-1 text-sm font-medium text-muted-foreground">
          <span className="truncate">{label}</span>
          {help ? <HelpTooltip>{help}</HelpTooltip> : null}
        </CardTitle>
        {Icon ? <Icon className="h-4 w-4 shrink-0 text-muted-foreground" /> : null}
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="break-keep text-2xl font-semibold leading-tight sm:text-3xl">{value}</div>
        {status ? <StatusBadge tone={tone}>{status}</StatusBadge> : null}
      </CardContent>
    </Card>
  );
}
