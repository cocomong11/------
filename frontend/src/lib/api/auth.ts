import { apiRequest, authHeader } from "./client";

export type AuthUser = {
  id: string;
  email: string;
  name: string;
};

export type AuthResponse = {
  tokenType: "Bearer";
  accessToken: string;
  user: AuthUser;
};

export type SignupRequest = {
  name: string;
  email: string;
  password: string;
};

export type LoginRequest = {
  email: string;
  password: string;
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
  me() {
    return apiRequest<AuthUser>("/auth/me", {
      headers: authHeader(),
    });
  },
};

