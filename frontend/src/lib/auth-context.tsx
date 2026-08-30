"use client";

import React, { createContext, useContext, useState, useEffect } from "react";
import { UserProfile } from "./types";
import { fetchAuthConfig } from "./api";

interface AuthContextType {
  currentUser: UserProfile | null;
  token: string | null;
  isAuthModalOpen: boolean;
  authModalMode: "SIGN_IN" | "SIGN_UP" | "ADMIN_LOGIN";
  authConfig: { isDev: boolean; supabaseUrl: string; supabaseAnonKey: string };
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
  const [authConfig, setAuthConfig] = useState({ isDev: true, supabaseUrl: "", supabaseAnonKey: "" });

  useEffect(() => {
    // Load config
    fetchAuthConfig().then(setAuthConfig);

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
    // If Dev mode or mock
    if (authConfig.isDev || !authConfig.supabaseUrl) {
      const isAdm = email.toLowerCase().includes("admin") || pass.toLowerCase().includes("admin");
      const role = isAdm ? "ADMIN" : "CUSTOMER";
      const name = email.split("@")[0].toUpperCase();
      const user: UserProfile = {
        id: "usr_" + email.split("@")[0],
        email,
        name,
        role,
        priority: isAdm ? 3 : 2,
      };
      setCurrentUser(user);
      setToken(null);
      localStorage.setItem("tf_user", JSON.stringify(user));
      localStorage.removeItem("tf_token");
      closeAuthModal();
      return { success: true };
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
    if (authConfig.isDev || !authConfig.supabaseUrl) {
      const user: UserProfile = {
        id: "usr_" + email.split("@")[0],
        email,
        name,
        role: "CUSTOMER",
        priority,
      };
      setCurrentUser(user);
      setToken(null);
      localStorage.setItem("tf_user", JSON.stringify(user));
      localStorage.removeItem("tf_token");
      closeAuthModal();
      return { success: true };
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

  const demoLogin = (email: string, name: string, role: "CUSTOMER" | "ADMIN", priority: number) => {
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
