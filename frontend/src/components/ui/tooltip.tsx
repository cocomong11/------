import * as React from "react";
import { cn } from "@/lib/utils";

function TooltipProvider({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}

function Tooltip({ children }: { children: React.ReactNode }) {
  return <span className="group relative inline-flex">{children}</span>;
}

function TooltipTrigger({
  children,
  className,
  ...props
}: React.HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cn("inline-flex cursor-help items-center focus-visible:outline-none", className)}
      tabIndex={0}
      {...props}
    >
      {children}
    </span>
  );
}

function TooltipContent({
  children,
  className,
  ...props
}: React.HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cn(
        "pointer-events-none absolute left-1/2 top-full z-50 mt-2 hidden w-64 -translate-x-1/2 rounded-md border bg-card px-3 py-2 text-xs font-normal leading-5 text-card-foreground shadow-md group-focus-within:inline-block group-hover:inline-block",
        className,
      )}
      role="tooltip"
      {...props}
    >
      {children}
    </span>
  );
}

export { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger };
