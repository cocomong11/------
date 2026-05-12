import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export function ErrorState({
  message,
  title = "요청을 처리하지 못했습니다",
}: {
  message: string;
  title?: string;
}) {
  return (
    <Alert variant="destructive">
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription>{message}</AlertDescription>
    </Alert>
  );
}
