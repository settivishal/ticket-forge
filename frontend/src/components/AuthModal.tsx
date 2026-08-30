"use client";

import React, { useState } from "react";
import { useAuth } from "@/lib/auth-context";
import { X, Sparkles, Shield, User, Lock, Mail, ArrowRight } from "lucide-react";

export function AuthModal() {
  const { isAuthModalOpen, authModalMode, closeAuthModal, signIn, signUp, demoLogin } = useAuth();

  const [activeTab, setActiveTab] = useState<"SIGN_IN" | "SIGN_UP" | "ADMIN">(
    authModalMode === "ADMIN_LOGIN" ? "ADMIN" : authModalMode === "SIGN_UP" ? "SIGN_UP" : "SIGN_IN"
  );

  // Sign In state
  const [loginEmail, setLoginEmail] = useState("alex@ticketforge.local");
  const [loginPassword, setLoginPassword] = useState("password123");

  // Sign Up state
  const [signupName, setSignupName] = useState("");
  const [signupEmail, setSignupEmail] = useState("");
  const [signupPassword, setSignupPassword] = useState("");
  const [signupPriority, setSignupPriority] = useState(3);

  // Admin state
  const [adminEmail, setAdminEmail] = useState("admin@ticketforge.local");
  const [adminSecret, setAdminSecret] = useState("admin_secret_key");

  // Loading & Error states
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isAuthModalOpen) return null;

  const handleSignInSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMsg(null);
    const res = await signIn(loginEmail, loginPassword);
    setIsLoading(false);
    if (!res.success) {
      setErrorMsg(res.error || "Sign in failed.");
    }
  };

  const handleSignUpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!signupName || !signupEmail || !signupPassword) {
      setErrorMsg("Please complete all registration fields.");
      return;
    }
    setIsLoading(true);
    setErrorMsg(null);
    const res = await signUp(signupName, signupEmail, signupPassword, signupPriority);
    setIsLoading(false);
    if (!res.success) {
      setErrorMsg(res.error || "Registration failed.");
    }
  };

  const handleAdminSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMsg(null);
    const res = await signIn(adminEmail, adminSecret);
    setIsLoading(false);
    if (!res.success) {
      setErrorMsg(res.error || "Invalid administrator credentials.");
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div
        className="relative w-full max-w-md overflow-hidden glass-panel rounded-2xl border border-white/10 shadow-2xl animate-scaleUp"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="flex items-center justify-between p-5 border-b border-white/10 bg-gradient-to-r from-blue-900/40 via-slate-900/60 to-slate-900/90">
          <div className="flex items-center gap-2">
            <div className="flex items-center justify-center w-9 h-9 rounded-xl bg-blue-600/30 border border-blue-500/40 text-blue-400">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white tracking-tight">TicketForge Account</h3>
              <p className="text-xs text-slate-400">Ultra-High Concurrency Flash-Sale Pass</p>
            </div>
          </div>
          <button
            onClick={closeAuthModal}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Tabs */}
        <div className="flex border-b border-white/10 bg-slate-950/50 p-1">
          <button
            type="button"
            onClick={() => {
              setActiveTab("SIGN_IN");
              setErrorMsg(null);
            }}
            className={`flex-1 py-2.5 text-xs font-semibold rounded-lg transition-all ${
              activeTab === "SIGN_IN"
                ? "bg-blue-600 text-white shadow-md"
                : "text-slate-400 hover:text-slate-200"
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab("SIGN_UP");
              setErrorMsg(null);
            }}
            className={`flex-1 py-2.5 text-xs font-semibold rounded-lg transition-all ${
              activeTab === "SIGN_UP"
                ? "bg-blue-600 text-white shadow-md"
                : "text-slate-400 hover:text-slate-200"
            }`}
          >
            Create Account
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab("ADMIN");
              setErrorMsg(null);
            }}
            className={`flex-1 py-2.5 text-xs font-semibold rounded-lg transition-all flex items-center justify-center gap-1 ${
              activeTab === "ADMIN"
                ? "bg-amber-600 text-white shadow-md"
                : "text-slate-400 hover:text-amber-400"
            }`}
          >
            <Shield className="w-3.5 h-3.5" />
            Admin
          </button>
        </div>

        {/* Error / Alert Message */}
        {errorMsg && (
          <div className="mx-5 mt-4 p-3 rounded-lg bg-red-500/15 border border-red-500/30 text-red-400 text-xs">
            {errorMsg}
          </div>
        )}

        {/* Tab 1: Sign In */}
        {activeTab === "SIGN_IN" && (
          <div className="p-6 space-y-4">
            <form onSubmit={handleSignInSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Email Address</label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                  <input
                    type="email"
                    required
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    placeholder="fan@ticketforge.local"
                    className="w-full pl-9 pr-3 py-2.5 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Password</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                  <input
                    type="password"
                    required
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-9 pr-3 py-2.5 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-3 btn-electric rounded-xl flex items-center justify-center gap-2 text-sm font-semibold"
              >
                <span>{isLoading ? "Signing in..." : "Sign In to TicketForge"}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </form>

            <div className="pt-3 border-t border-white/10">
              <span className="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-2">
                1-Click Quick Demo Profiles:
              </span>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => demoLogin("alex@ticketforge.local", "Alex Miller", "CUSTOMER", 3)}
                  className="p-2.5 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-white/10 hover:border-amber-500/50 text-left transition-all group"
                >
                  <div className="text-xs font-bold text-amber-400 group-hover:text-amber-300 flex items-center gap-1">
                    ⭐ Alex (VIP)
                  </div>
                  <div className="text-[10px] text-slate-400">Tier 3 VIP Presale</div>
                </button>
                <button
                  type="button"
                  onClick={() => demoLogin("jordan@ticketforge.local", "Jordan Lee", "CUSTOMER", 1)}
                  className="p-2.5 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-white/10 hover:border-emerald-500/50 text-left transition-all group"
                >
                  <div className="text-xs font-bold text-emerald-400 group-hover:text-emerald-300 flex items-center gap-1">
                    🟢 Jordan (Fan)
                  </div>
                  <div className="text-[10px] text-slate-400">Tier 1 Standard</div>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Create Account */}
        {activeTab === "SIGN_UP" && (
          <div className="p-6 space-y-4">
            <form onSubmit={handleSignUpSubmit} className="space-y-3.5">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Full Name / Fan Handle</label>
                <div className="relative">
                  <User className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    required
                    value={signupName}
                    onChange={(e) => setSignupName(e.target.value)}
                    placeholder="e.g. Taylor Fan"
                    className="w-full pl-9 pr-3 py-2 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Email Address</label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                  <input
                    type="email"
                    required
                    value={signupEmail}
                    onChange={(e) => setSignupEmail(e.target.value)}
                    placeholder="fan@example.com"
                    className="w-full pl-9 pr-3 py-2 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Create Password</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                  <input
                    type="password"
                    required
                    value={signupPassword}
                    onChange={(e) => setSignupPassword(e.target.value)}
                    placeholder="At least 6 characters"
                    className="w-full pl-9 pr-3 py-2 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Priority Presale Tier</label>
                <select
                  value={signupPriority}
                  onChange={(e) => setSignupPriority(parseInt(e.target.value, 10))}
                  className="w-full px-3 py-2 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500"
                >
                  <option value={3}>⭐ Tier 3 — VIP Gold Member</option>
                  <option value={2}>💎 Tier 2 — Premium Presale Pass</option>
                  <option value={1}>🟢 Tier 1 — Standard General Fan</option>
                </select>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-3 btn-electric rounded-xl flex items-center justify-center gap-2 text-sm font-semibold mt-2"
              >
                <span>{isLoading ? "Creating Account..." : "Create Account & Unlock Seats"}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </form>
          </div>
        )}

        {/* Tab 3: Admin Gateway */}
        {activeTab === "ADMIN" && (
          <div className="p-6 space-y-4">
            <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs">
              🔒 <strong>Administrator Gateway:</strong> Access venue capacity initialization, batch user cancellations, and high-concurrency burst simulators.
            </div>

            <form onSubmit={handleAdminSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Admin Email</label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                  <input
                    type="email"
                    required
                    value={adminEmail}
                    onChange={(e) => setAdminEmail(e.target.value)}
                    className="w-full pl-9 pr-3 py-2.5 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Admin Access Secret</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                  <input
                    type="password"
                    required
                    value={adminSecret}
                    onChange={(e) => setAdminSecret(e.target.value)}
                    className="w-full pl-9 pr-3 py-2.5 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-3 bg-gradient-to-r from-amber-600 to-amber-700 hover:from-amber-500 hover:to-amber-600 text-white rounded-xl flex items-center justify-center gap-2 text-sm font-semibold shadow-lg shadow-amber-900/30"
              >
                <span>{isLoading ? "Authenticating..." : "Sign In to Admin Operations"}</span>
                <Shield className="w-4 h-4" />
              </button>
            </form>

            <div className="pt-2 text-center">
              <button
                type="button"
                onClick={() => demoLogin("admin@ticketforge.local", "Venue Operations Admin", "ADMIN", 3)}
                className="text-xs text-amber-400/80 hover:text-amber-300 underline"
              >
                ⚡ 1-Click Fast Admin Sign In (Demo Mode)
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
