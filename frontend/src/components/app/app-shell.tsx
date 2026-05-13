import { ContentContainer } from "@/components/app/content-container";
import { Header } from "@/components/app/header";
import { Sidebar } from "@/components/app/sidebar";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { LEGAL_NOTICE } from "@/lib/constants";

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-background lg:flex">
      <Sidebar />
      <div className="min-w-0 flex-1">
        <Header />
        <ContentContainer>
          {children}
          <Alert className="legal-disclaimer">
            <AlertDescription>{LEGAL_NOTICE}</AlertDescription>
          </Alert>
        </ContentContainer>
      </div>
    </div>
  );
}
