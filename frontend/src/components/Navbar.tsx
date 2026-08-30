"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { Ticket, Shield, Sparkles, User, LogOut, Compass, Radio } from "lucide-react";
import { fetchSystemStatus } from "@/lib/api";

export function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const { currentUser, openAuthModal, signOut } = useAuth();
  const [isOnline, setIsOnline] = useState(true);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);

  useEffect(() => {
    fetchSystemStatus().then((status) => {
      setIsOnline(!!status);
    });
    const interval = setInterval(() => {
      fetchSystemStatus().then((status) => setIsOnline(!!status));
    }, 15000);
    return () => clearInterval(interval);
  }, []);

  const handleAdminNav = (e: React.MouseEvent) => {
    if (!currentUser || currentUser.role !== "ADMIN") {
      e.preventDefault();
      openAuthModal("ADMIN_LOGIN");
    }
  };

  const handleWalletNav = (e: React.MouseEvent) => {
    if (!currentUser) {
      e.preventDefault();
      openAuthModal("SIGN_IN");
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full border-b border-white/10 bg-slate-950/80 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo & Tag */}
          <div className="flex items-center gap-6">
            <Link href="/" className="flex items-center gap-2.5 group">
              <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-700 via-blue-600 to-indigo-500 text-white shadow-lg shadow-blue-900/30 group-hover:scale-105 transition-transform">
                <Ticket className="w-5 h-5" />
              </div>
              <div className="flex flex-col">
                <span className="text-xl font-black tracking-tight text-white flex items-center gap-1.5">
                  TICKET<span className="text-blue-500">FORGE</span>
                </span>
                <span className="text-[10px] uppercase font-bold tracking-widest text-slate-400">
                  Virtual Threads Engine
                </span>
              </div>
            </Link>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center gap-1">
              <Link
                href="/events"
                className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-1.5 ${
                  pathname === "/events"
                    ? "bg-blue-600/20 text-blue-400 border border-blue-500/30"
                    : "text-slate-300 hover:text-white hover:bg-white/5"
                }`}
              >
                <Compass className="w-4 h-4" />
                Discover Tours
              </Link>
              <Link
                href="/events/cyber-symphony-2026"
                className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-1.5 ${
                  pathname.startsWith("/events/") && pathname !== "/events"
                    ? "bg-blue-600/20 text-blue-400 border border-blue-500/30"
                    : "text-slate-300 hover:text-white hover:bg-white/5"
                }`}
              >
                <Sparkles className="w-4 h-4 text-amber-400" />
                Live Arena Seating
              </Link>
              <Link
                href="/wallet"
                onClick={handleWalletNav}
                className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-1.5 ${
                  pathname === "/wallet"
                    ? "bg-blue-600/20 text-blue-400 border border-blue-500/30"
                    : "text-slate-300 hover:text-white hover:bg-white/5"
                }`}
              >
                <Ticket className="w-4 h-4" />
                My Passes
              </Link>
              <Link
                href="/admin"
                onClick={handleAdminNav}
                className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-1.5 ${
                  pathname === "/admin"
                    ? "bg-amber-600/20 text-amber-400 border border-amber-500/30"
                    : "text-slate-300 hover:text-amber-400 hover:bg-amber-950/20"
                }`}
              >
                <Shield className="w-4 h-4 text-amber-400" />
                Admin Console
              </Link>
            </nav>
          </div>

          {/* Right Action Controls: Live Status & User Profile */}
          <div className="flex items-center gap-3">
            {/* Live SSE Telemetry Badge */}
            <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-900/90 border border-white/10 text-xs text-slate-300">
              <span className="relative flex h-2 w-2">
                <span
                  className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
                    isOnline ? "bg-emerald-400" : "bg-red-400"
                  }`}
                ></span>
                <span
                  className={`relative inline-flex rounded-full h-2 w-2 ${
                    isOnline ? "bg-emerald-500" : "bg-red-500"
                  }`}
                ></span>
              </span>
              <span className="font-medium text-[11px]">
                {isOnline ? "Engine 10K RPS Ready" : "Reconnecting Engine"}
              </span>
            </div>

            {/* User State */}
            {currentUser ? (
              <div className="relative">
                <button
                  onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                  className="flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-slate-900 border border-white/10 hover:border-white/20 transition-all text-left"
                >
                  <div
                    className={`flex items-center justify-center w-8 h-8 rounded-lg font-bold text-xs ${
                      currentUser.role === "ADMIN"
                        ? "bg-amber-600 text-white"
                        : "bg-gradient-to-tr from-blue-600 to-indigo-600 text-white"
                    }`}
                  >
                    {currentUser.role === "ADMIN" ? "👑" : currentUser.name.charAt(0).toUpperCase()}
                  </div>
                  <div className="hidden sm:flex flex-col">
                    <span className="text-xs font-bold text-white truncate max-w-[120px]">
                      {currentUser.name}
                    </span>
                    <span
                      className={`text-[10px] font-semibold ${
                        currentUser.role === "ADMIN"
                          ? "text-amber-400"
                          : currentUser.priority === 3
                          ? "text-amber-400"
                          : "text-blue-400"
                      }`}
                    >
                      {currentUser.role === "ADMIN"
                        ? "👑 Venue Operator"
                        : currentUser.priority === 3
                        ? "⭐ VIP Member"
                        : "🟢 Standard Fan"}
                    </span>
                  </div>
                </button>

                {/* Dropdown Menu */}
                {isUserMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 rounded-xl glass-panel border border-white/10 shadow-xl py-1.5 z-50 animate-fadeIn">
                    <div className="px-4 py-2 border-b border-white/10">
                      <p className="text-xs font-semibold text-white">{currentUser.name}</p>
                      <p className="text-[10px] text-slate-400 truncate">{currentUser.email}</p>
                    </div>
                    <Link
                      href="/wallet"
                      onClick={() => setIsUserMenuOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-xs text-slate-300 hover:text-white hover:bg-white/10 transition-colors"
                    >
                      <Ticket className="w-3.5 h-3.5" />
                      My Digital Passes
                    </Link>
                    {currentUser.role === "ADMIN" && (
                      <Link
                        href="/admin"
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-xs text-amber-300 hover:text-amber-200 hover:bg-amber-950/30 transition-colors"
                      >
                        <Shield className="w-3.5 h-3.5" />
                        Admin Operations
                      </Link>
                    )}
                    <button
                      onClick={() => {
                        signOut();
                        setIsUserMenuOpen(false);
                        router.push("/");
                      }}
                      className="w-full flex items-center gap-2 px-4 py-2 text-xs text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-colors text-left"
                    >
                      <LogOut className="w-3.5 h-3.5" />
                      Sign Out
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <button
                  onClick={() => openAuthModal("SIGN_IN")}
                  className="px-3.5 py-2 text-xs font-semibold text-slate-200 hover:text-white rounded-xl hover:bg-white/5 transition-colors"
                >
                  Sign In
                </button>
                <button
                  onClick={() => openAuthModal("SIGN_UP")}
                  className="px-4 py-2 text-xs font-semibold btn-electric rounded-xl flex items-center gap-1.5"
                >
                  <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                  <span>Register</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
