import { apiRequest, authHeader } from "./client";

export type UserStatus = "PENDING_EMAIL_VERIFICATION" | "ACTIVE" | "SUSPENDED" | "DELETED";

export type AuthUser = {
  id: string;
  email: string;
  name: string;
  status: UserStatus;
  emailVerified: boolean;
  requiredAgreementsAccepted: boolean;
  lastLoginAt: string | null;
};

export type AuthResponse = {
  tokenType: "Bearer";
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser;
};

export type SignupRequest = {
  name: string;
  email: string;
  password: string;
  termsAgreed: boolean;
  privacyAgreed: boolean;
  businessInfoConsentAgreed: boolean;
  taxDataConsentAgreed: boolean;
  referenceNoticeAgreed: boolean;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type MessageResponse = {
  message: string;
};

export type OAuthProvider = "google" | "kakao";

export type OAuthAuthorizationResponse = {
  provider: OAuthProvider;
  configured: boolean;
  authorizationUrl: string | null;
  message: string;
};

export const authApi = {
  signup(request: SignupRequest) {
    return apiRequest<AuthResponse>("/auth/signup", {
      method: "POST",
      body: JSON.stringify(request),
    });
  },
  login(request: LoginRequest) {
    return apiRequest<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(request),
    });
  },
  verifyEmail(request: { email: string; code: string }) {
    return apiRequest<AuthResponse>("/auth/verify-email", {
      method: "POST",
      body: JSON.stringify(request),
    });
  },
  resendVerificationCode(email: string) {
    return apiRequest<MessageResponse>("/auth/resend-verification-code", {
      method: "POST",
      body: JSON.stringify({ email }),
    });
  },
  refresh(refreshToken: string) {
    return apiRequest<AuthResponse>("/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    });
  },
  logout(refreshToken: string) {
    return apiRequest<MessageResponse>("/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    });
  },
  forgotPassword(email: string) {
    return apiRequest<MessageResponse>("/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify({ email }),
    });
  },
  resetPassword(request: { token: string; newPassword: string }) {
    return apiRequest<MessageResponse>("/auth/reset-password", {
      method: "POST",
      body: JSON.stringify(request),
    });
  },
  oauthAuthorizationUrl(provider: OAuthProvider, redirectUri: string) {
    return apiRequest<OAuthAuthorizationResponse>(
      `/auth/oauth/${provider}/authorization-url?redirectUri=${encodeURIComponent(redirectUri)}`,
    );
  },
  me() {
    return apiRequest<AuthUser>("/auth/me", {
      headers: authHeader(),
    });
  },
};
