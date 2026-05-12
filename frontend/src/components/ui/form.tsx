import * as React from "react";
import { FormProvider, type FieldValues, type UseFormReturn } from "react-hook-form";
import { cn } from "@/lib/utils";

function Form<TFieldValues extends FieldValues>({
  children,
  ...props
}: UseFormReturn<TFieldValues> & { children: React.ReactNode }) {
  return <FormProvider {...props}>{children}</FormProvider>;
}

function FormItem({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("space-y-2", className)} {...props} />;
}

function FormDescription({ className, ...props }: React.HTMLAttributes<HTMLParagraphElement>) {
  return <p className={cn("text-xs leading-5 text-muted-foreground", className)} {...props} />;
}

function FormMessage({ className, ...props }: React.HTMLAttributes<HTMLParagraphElement>) {
  return <p className={cn("text-sm text-destructive", className)} {...props} />;
}

export { Form, FormDescription, FormItem, FormMessage };
