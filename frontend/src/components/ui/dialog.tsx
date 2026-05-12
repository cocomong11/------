"use client";

import * as React from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type DialogContextValue = {
  open: boolean;
  setOpen: (open: boolean) => void;
};

const DialogContext = React.createContext<DialogContextValue | null>(null);

function Dialog({
  children,
  open,
  onOpenChange,
}: {
  children: React.ReactNode;
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}) {
  const [internalOpen, setInternalOpen] = React.useState(false);
  const controlled = open !== undefined;
  const value = React.useMemo(
    () => ({
      open: controlled ? open : internalOpen,
      setOpen: (nextOpen: boolean) => {
        if (!controlled) {
          setInternalOpen(nextOpen);
        }
        onOpenChange?.(nextOpen);
      },
    }),
    [controlled, internalOpen, onOpenChange, open],
  );

  return <DialogContext.Provider value={value}>{children}</DialogContext.Provider>;
}

function DialogTrigger({
  children,
  asChild = false,
}: {
  children: React.ReactElement<{ onClick?: React.MouseEventHandler }>;
  asChild?: boolean;
}) {
  const context = useDialogContext();
  if (asChild) {
    return React.cloneElement(children, {
      onClick: (event: React.MouseEvent) => {
        children.props.onClick?.(event);
        context.setOpen(true);
      },
    });
  }
  return <button onClick={() => context.setOpen(true)}>{children}</button>;
}

function DialogContent({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  const context = useDialogContext();
  if (!context.open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/40 p-4">
      <div
        className={cn(
          "relative max-h-[90vh] w-full max-w-lg overflow-auto rounded-md border bg-card p-6 text-card-foreground shadow-lg",
          className,
        )}
      >
        <Button
          aria-label="닫기"
          className="absolute right-3 top-3"
          onClick={() => context.setOpen(false)}
          size="icon"
          type="button"
          variant="ghost"
        >
          <X className="h-4 w-4" />
        </Button>
        {children}
      </div>
    </div>
  );
}

function DialogHeader({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("mb-4 space-y-2 pr-8", className)} {...props} />;
}

function DialogTitle({ className, ...props }: React.HTMLAttributes<HTMLHeadingElement>) {
  return <h2 className={cn("text-lg font-semibold tracking-normal", className)} {...props} />;
}

function DialogDescription({ className, ...props }: React.HTMLAttributes<HTMLParagraphElement>) {
  return <p className={cn("text-sm leading-6 text-muted-foreground", className)} {...props} />;
}

function useDialogContext() {
  const context = React.useContext(DialogContext);
  if (!context) {
    throw new Error("Dialog components must be used within Dialog");
  }
  return context;
}

export { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger };
