import * as React from "react";
import { FileQuestion } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

export function EmptyState({
  action,
  description,
  icon: Icon = FileQuestion,
  title,
}: {
  action?: React.ReactNode;
  description: string;
  icon?: React.ComponentType<{ className?: string }>;
  title: string;
}) {
  return (
    <Card>
      <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-lg bg-secondary">
          <Icon className="h-6 w-6 text-muted-foreground" />
        </span>
        <div>
          <h2 className="text-[14px] font-semibold tracking-normal text-slate-900">{title}</h2>
          <p className="mt-2 max-w-md text-[13px] leading-6 text-muted-foreground">{description}</p>
        </div>
        {action ? <div>{action}</div> : null}
      </CardContent>
    </Card>
  );
}

export function EmptyStateAction({ children, ...props }: React.ComponentProps<typeof Button>) {
  return <Button {...props}>{children}</Button>;
}
