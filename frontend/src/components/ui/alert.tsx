import * as React from "react";
import { cn } from "@/lib/utils";

type AlertVariant = "default" | "success" | "warning" | "destructive";

const variantClassName: Record<AlertVariant, string> = {
  default: "border-border bg-card text-card-foreground",
  success: "border-emerald-100 bg-emerald-50 text-emerald-900",
  warning: "border-amber-100 bg-amber-50 text-amber-900",
  destructive: "border-red-100 bg-red-50 text-red-700",
};

export interface AlertProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: AlertVariant;
}

const Alert = React.forwardRef<HTMLDivElement, AlertProps>(
  ({ className, variant = "default", ...props }, ref) => (
    <div
      ref={ref}
      className={cn("relative w-full rounded-lg border px-4 py-3 text-[13px] leading-6", variantClassName[variant], className)}
      role="status"
      {...props}
    />
  ),
);
Alert.displayName = "Alert";

const AlertTitle = React.forwardRef<HTMLHeadingElement, React.HTMLAttributes<HTMLHeadingElement>>(
  ({ className, ...props }, ref) => (
    <h5 ref={ref} className={cn("mb-1 font-semibold leading-none tracking-normal", className)} {...props} />
  ),
);
AlertTitle.displayName = "AlertTitle";

const AlertDescription = React.forwardRef<HTMLParagraphElement, React.HTMLAttributes<HTMLParagraphElement>>(
  ({ className, ...props }, ref) => <p ref={ref} className={cn("text-[13px] leading-6", className)} {...props} />,
);
AlertDescription.displayName = "AlertDescription";

export { Alert, AlertDescription, AlertTitle };
