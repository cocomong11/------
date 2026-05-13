import * as React from "react";
import { cn } from "@/lib/utils";

export function ContentContainer({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return <main className={cn("min-w-0 space-y-6 px-4 py-5 lg:px-7 lg:py-6", className)}>{children}</main>;
}
