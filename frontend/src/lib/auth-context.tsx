"use client";

import React, { createContext, useContext, useState, useEffect } from "react";
import { UserProfile } from "./types";
import { fetchAuthConfig, AuthConfig } from "./api";

interface AuthContextType {
  currentUser: UserProfile | null;
  token: string | null;
  isAuthModalOpen: boolean;
  authModalMode: "SIGN_IN" | "SIGN_UP" | "ADMIN_LOGIN";
  authConfig: AuthConfig | null;
  isAuthReady: boolean;
  openAuthModal: (mode?: "SIGN_IN" | "SIGN_UP" | "ADMIN_LOGIN") => void;
  closeAuthModal: () => void;
  signIn: (email: string, pass: string) => Promise<{ success: boolean; error?: string }>;
  signUp: (name: string, email: string, pass: string, priority: number) => Promise<{ success: boolean; error?: string }>;
  demoLogin: (email: string, name: string, role: "CUSTOMER" | "ADMIN", priority: number) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState<"SIGN_IN" | "SIGN_UP" | "ADMIN_LOGIN">("SIGN_IN");
  // Starts null and stays null if the backend is unreachable, so sign-in fails
  // closed rather than falling back to the local mock path.
  const [authConfig, setAuthConfig] = useState<AuthConfig | null>(null);
  const [isAuthReady, setIsAuthReady] = useState(false);

  useEffect(() => {
    // Load config
    fetchAuthConfig()
      .then(setAuthConfig)
      .finally(() => setIsAuthReady(true));

    // Restore user session if present
    try {
      const savedUser = localStorage.getItem("tf_user");
      const savedToken = localStorage.getItem("tf_token");
      if (savedUser) setCurrentUser(JSON.parse(savedUser));
      if (savedToken) setToken(savedToken);
    } catch (err) {
      console.error("Session restore error:", err);
    }
  }, []);

  const openAuthModal = (mode: "SIGN_IN" | "SIGN_UP" | "ADMIN_LOGIN" = "SIGN_IN") => {
    setAuthModalMode(mode);
    setIsAuthModalOpen(true);
  };

  const closeAuthModal = () => setIsAuthModalOpen(false);

  const signIn = async (email: string, pass: string): Promise<{ success: boolean; error?: string }> => {
    if (!authConfig) {
      return { success: false, error: "Authentication is still initializing. Please try again." };
    }
    if (!authConfig.supabaseUrl || !authConfig.supabaseAnonKey) {
      return { success: false, error: "Authentication is not configured on the server." };
    }

    // Supabase GoTrue Auth
    try {
      const res = await fetch(`${authConfig.supabaseUrl}/auth/v1/token?grant_type=password`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          apikey: authConfig.supabaseAnonKey,
        },
        body: JSON.stringify({ email, password: pass }),
      });
      const json = await res.json();
      if (res.ok && json.access_token) {
        const u = json.user || {};
        const meta = u.user_metadata || {};
        const appMeta = u.app_metadata || {};
        const role = appMeta.role === "ROLE_ADMIN" || meta.role === "admin" ? "ADMIN" : "CUSTOMER";
        const profile: UserProfile = {
          id: u.id || "usr_" + email.split("@")[0],
          email: u.email || email,
          name: meta.name || email.split("@")[0],
          role,
          priority: parseInt(meta.priority_tier, 10) || 1,
        };

        setCurrentUser(profile);
        setToken(json.access_token);
        localStorage.setItem("tf_user", JSON.stringify(profile));
        localStorage.setItem("tf_token", json.access_token);
        closeAuthModal();
        return { success: true };
      }
      return { success: false, error: json.error_description || json.msg || "Invalid credentials." };
    } catch (err) {
      console.error(err);
      return { success: false, error: "Connection error contacting authentication server." };
    }
  };

  const signUp = async (
    name: string,
    email: string,
    pass: string,
    priority: number
  ): Promise<{ success: boolean; error?: string }> => {
    if (!authConfig) {
      return { success: false, error: "Authentication is still initializing. Please try again." };
    }
    if (!authConfig.supabaseUrl || !authConfig.supabaseAnonKey) {
      return { success: false, error: "Authentication is not configured on the server." };
    }

    try {
      const res = await fetch(`${authConfig.supabaseUrl}/auth/v1/signup`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          apikey: authConfig.supabaseAnonKey,
        },
        body: JSON.stringify({
          email,
          password: pass,
          data: { name, priority_tier: priority, role: "ROLE_CUSTOMER" },
        }),
      });
      const json = await res.json();
      if (res.ok) {
        if (json.access_token) {
          const profile: UserProfile = {
            id: json.user?.id || "usr_" + email.split("@")[0],
            email,
            name,
            role: "CUSTOMER",
            priority,
          };
          setCurrentUser(profile);
          setToken(json.access_token);
          localStorage.setItem("tf_user", JSON.stringify(profile));
          localStorage.setItem("tf_token", json.access_token);
          closeAuthModal();
          return { success: true };
        }
        return { success: true, error: "Account created! Please check your email to verify." };
      }
      return { success: false, error: json.error_description || json.msg || "Registration failed." };
    } catch (err) {
      console.error(err);
      return { success: false, error: "Failed to connect to authentication server." };
    }
  };

  // Local development convenience only. The backend reports whether it is running
  // the dev profile; outside dev this is a no-op, and the backend's real
  // authorization rules apply regardless of what is cached client-side.
  const demoLogin = (email: string, name: string, role: "CUSTOMER" | "ADMIN", priority: number) => {
    if (!authConfig?.isDev) {
      console.warn("demoLogin is only available when the backend runs the dev profile.");
      return;
    }
    const user: UserProfile = {
      id: "usr_" + email.split("@")[0],
      email,
      name,
      role,
      priority,
    };
    setCurrentUser(user);
    setToken(null);
    localStorage.setItem("tf_user", JSON.stringify(user));
    localStorage.removeItem("tf_token");
    closeAuthModal();
  };

  const signOut = () => {
    setCurrentUser(null);
    setToken(null);
    localStorage.removeItem("tf_user");
    localStorage.removeItem("tf_token");
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        token,
        isAuthModalOpen,
        authModalMode,
        authConfig,
        isAuthReady,
        openAuthModal,
        closeAuthModal,
        signIn,
        signUp,
        demoLogin,
        signOut,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
