"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

export function ConfirmDialog({
  cancelLabel = "취소",
  children,
  confirmLabel = "확인",
  description,
  onConfirm,
  title,
}: {
  cancelLabel?: string;
  children: React.ReactElement<{ onClick?: React.MouseEventHandler }>;
  confirmLabel?: string;
  description: string;
  onConfirm: () => void;
  title: string;
}) {
  const [open, setOpen] = useState(false);

  function confirm() {
    onConfirm();
    setOpen(false);
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <div className="flex justify-end gap-2">
          <Button onClick={() => setOpen(false)} type="button" variant="outline">
            {cancelLabel}
          </Button>
          <Button onClick={confirm} type="button">
            {confirmLabel}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
