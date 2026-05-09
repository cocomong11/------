import * as React from "react";
import { cn } from "@/lib/utils";

export interface ProgressProps extends React.HTMLAttributes<HTMLDivElement> {
  value?: number;
}

const Progress = React.forwardRef<HTMLDivElement, ProgressProps>(
  ({ className, value = 0, ...props }, ref) => {
    const clampedValue = Math.max(0, Math.min(100, value));

    return (
      <div
        ref={ref}
        aria-valuemax={100}
        aria-valuemin={0}
        aria-valuenow={clampedValue}
        className={cn("relative h-3 w-full overflow-hidden rounded-full bg-secondary", className)}
        role="progressbar"
        {...props}
      >
        <div
          className="h-full rounded-full bg-primary transition-all"
          style={{ width: `${clampedValue}%` }}
        />
      </div>
    );
  },
);
Progress.displayName = "Progress";

export { Progress };
