import * as React from "react";
import { cn } from "@/lib/utils";

export function PageTitle({
  actions,
  className,
  description,
  eyebrow,
  title,
}: {
  actions?: React.ReactNode;
  className?: string;
  description?: string;
  eyebrow?: string;
  title: string;
}) {
  return (
    <div className={cn("flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between", className)}>
      <div className="min-w-0">
        {eyebrow ? <p className="mb-1.5 text-[10px] font-semibold uppercase tracking-[0.08em] text-primary">{eyebrow}</p> : null}
        <h1 className="text-[18px] font-bold tracking-normal text-slate-900">{title}</h1>
        {description ? <p className="mt-1.5 max-w-3xl text-[13px] leading-6 text-muted-foreground">{description}</p> : null}
      </div>
      {actions ? <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}
