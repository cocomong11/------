import { ContentContainer } from "@/components/app/content-container";
import { Header } from "@/components/app/header";
import { Sidebar } from "@/components/app/sidebar";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { LEGAL_NOTICE } from "@/lib/constants";

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="container grid gap-6 py-6 md:grid-cols-[240px_1fr]">
        <Sidebar />
        <ContentContainer>
          {children}
          <Alert>
            <AlertDescription>{LEGAL_NOTICE}</AlertDescription>
          </Alert>
        </ContentContainer>
      </div>
    </div>
  );
}
