"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@/lib/auth-context";
import {
  fetchSystemStatus,
  adminInitializeVenue,
  adminExpandVenue,
  adminReleaseRange,
  fetchAdminWaitlist,
  promoteWaitlistUser,
  removeWaitlistUser,
  reserveSeat,
} from "@/lib/api";
import { SystemStatus, WaitlistEntry } from "@/lib/types";
import {
  Shield,
  Activity,
  Layers,
  Users,
  Flame,
  Zap,
  RefreshCw,
  PlusCircle,
  RotateCcw,
  Scissors,
  CheckCircle2,
  Lock,
} from "lucide-react";

export default function AdminConsolePage() {
  const { currentUser, token, openAuthModal } = useAuth();

  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null);
  const [waitlist, setWaitlist] = useState<WaitlistEntry[]>([]);
  const [initCount, setInitCount] = useState(50);
  const [expandCount, setExpandCount] = useState(10);
  const [releaseFrom, setReleaseFrom] = useState("burst_fan_1000");
  const [releaseTo, setReleaseTo] = useState("burst_fan_9999");
  const [burstStatus, setBurstStatus] = useState<string | null>(null);
  const [isBursting, setIsBursting] = useState(false);
  const [actionAlert, setActionAlert] = useState<{ text: string; type: "success" | "info" } | null>(null);

  const showAlert = (text: string, type: "success" | "info" = "success") => {
    setActionAlert({ text, type });
    setTimeout(() => setActionAlert(null), 4000);
  };

  const loadData = async () => {
    if (currentUser?.role !== "ADMIN") return;
    const [st, wl] = await Promise.all([fetchSystemStatus(), fetchAdminWaitlist(token, currentUser)]);
    setSystemStatus(st);
    setWaitlist(wl);
  };

  useEffect(() => {
    if (currentUser?.role === "ADMIN") {
      loadData();
    }
  }, [currentUser, token]);

  const handleInitVenue = async () => {
    const ok = await adminInitializeVenue(initCount, token, currentUser);
    if (ok) {
      showAlert(`🏟️ Venue reset and re-indexed with ${initCount} available seats!`);
      loadData();
    }
  };

  const handleExpandVenue = async () => {
    const ok = await adminExpandVenue(expandCount, token, currentUser);
    if (ok) {
      showAlert(`➕ Added +${expandCount} seats! Waiting queue auto-fulfilled.`);
      loadData();
    }
  };

  const handleReleaseRange = async () => {
    if (!releaseFrom || !releaseTo) return;
    const res = await adminReleaseRange(releaseFrom, releaseTo, token, currentUser);
    if (res.success) {
      showAlert(`✂️ Released ${res.count} seats in range [${releaseFrom}, ${releaseTo}].`);
      loadData();
    }
  };

  const handlePromoteUser = async (userId: string, currentPriority: number) => {
    const ok = await promoteWaitlistUser(userId, currentPriority + 1, token, currentUser);
    if (ok) {
      showAlert(`⭐ Promoted ${userId} to higher queue priority!`);
      loadData();
    }
  };

  const handleRemoveUser = async (userId: string) => {
    const ok = await removeWaitlistUser(userId, token, currentUser);
    if (ok) {
      showAlert(`Removed ${userId} from waiting queue.`);
      loadData();
    }
  };

  const handleBurstSimulator = async () => {
    setIsBursting(true);
    setBurstStatus("Firing 10 concurrent requests across virtual threads...");

    const promises = [];
    for (let i = 1; i <= 10; i++) {
      const uid = "burst_fan_" + Math.floor(Math.random() * 9000 + 1000);
      const prio = (i % 3) + 1;
      promises.push(reserveSeat(uid, prio, token, currentUser));
    }

    await Promise.allSettled(promises);
    setBurstStatus("✅ Burst completed! Inventory & waitlist updated in real time.");
    setIsBursting(false);
    loadData();
  };

  // RBAC Access Guard: If not admin, show barrier
  if (!currentUser || currentUser.role !== "ADMIN") {
    return (
      <div className="max-w-md mx-auto px-4 py-24 text-center space-y-6">
        <div className="w-16 h-16 rounded-3xl bg-amber-500/20 border border-amber-500/30 text-amber-400 flex items-center justify-center mx-auto">
          <Lock className="w-8 h-8" />
        </div>
        <div className="space-y-2">
          <h1 className="text-2xl font-black text-white">Administrator Access Required</h1>
          <p className="text-xs text-slate-400 leading-relaxed">
            The Admin Operations Command Center is protected by strict role-based access control. Please sign in with administrator credentials to proceed.
          </p>
        </div>
        <button
          onClick={() => openAuthModal("ADMIN_LOGIN")}
          className="px-6 py-3 bg-amber-600 hover:bg-amber-500 text-white rounded-xl text-xs font-bold shadow-lg shadow-amber-900/40"
        >
          Sign In as Venue Administrator
        </button>
      </div>
    );
  }

  const total = systemStatus?.totalSeats || 1;
  const reservedPct = Math.round(((systemStatus?.reservedSeats || 0) / total) * 100);
  const heldPct = Math.round(((systemStatus?.heldSeats || 0) / total) * 100);
  const availablePct = Math.round(((systemStatus?.availableSeats || 0) / total) * 100);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 w-full space-y-8">
      {/* Alert Banner */}
      {actionAlert && (
        <div className="p-4 rounded-2xl bg-blue-500/15 border border-blue-500/30 text-blue-300 text-xs font-semibold flex items-center gap-2 animate-fadeIn">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{actionAlert.text}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-6">
        <div>
          <div className="flex items-center gap-2 text-xs font-bold text-amber-400 uppercase tracking-wider mb-1">
            <Shield className="w-4 h-4" />
            <span>Venue Operations Command Center</span>
          </div>
          <h1 className="text-3xl font-black text-white tracking-tight">
            Engine Telemetry &amp; Inventory Controls
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Managing Spring Boot 3.3 Virtual Threads, Red-Black Tree inventory, and Redis state locks.
          </p>
        </div>

        <button
          onClick={loadData}
          className="px-4 py-2.5 rounded-xl bg-slate-900 border border-white/10 hover:border-white/20 text-xs font-semibold text-slate-200 flex items-center gap-2 self-start sm:self-auto"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Refresh Telemetry</span>
        </button>
      </div>

      {/* 1. Real-Time Telemetry KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-panel p-5 rounded-2xl border border-white/10 space-y-2">
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Available Seats</div>
          <div className="text-3xl font-black text-emerald-400">{systemStatus?.availableSeats ?? 0}</div>
          <div className="text-[10px] text-slate-500">Unallocated in Red-Black Tree</div>
        </div>

        <div className="glass-panel p-5 rounded-2xl border border-white/10 space-y-2">
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Confirmed Bookings</div>
          <div className="text-3xl font-black text-blue-400">{systemStatus?.reservedSeats ?? 0}</div>
          <div className="text-[10px] text-slate-500">Guaranteed admissions</div>
        </div>

        <div className="glass-panel p-5 rounded-2xl border border-white/10 space-y-2">
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Active Seat Holds</div>
          <div className="text-3xl font-black text-amber-400">{systemStatus?.heldSeats ?? 0}</div>
          <div className="text-[10px] text-slate-500">60s TTL countdowns active</div>
        </div>

        <div className="glass-panel p-5 rounded-2xl border border-white/10 space-y-2">
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Waitlist Queue</div>
          <div className="text-3xl font-black text-purple-400">{systemStatus?.waitlistCount ?? 0}</div>
          <div className="text-[10px] text-slate-500">Min-Heap priority sorted</div>
        </div>
      </div>

      {/* Occupancy Bar */}
      <div className="glass-panel p-5 rounded-2xl border border-white/10 space-y-2">
        <div className="flex justify-between text-xs font-semibold">
          <span className="text-white">Venue Capacity Utilization</span>
          <span className="text-slate-300">
            {reservedPct + heldPct}% Booked ({systemStatus?.totalSeats ?? 0} Total Seats)
          </span>
        </div>
        <div className="w-full h-3 rounded-full bg-slate-900 overflow-hidden flex">
          <div style={{ width: `${reservedPct}%` }} className="h-full bg-blue-500 transition-all duration-500"></div>
          <div style={{ width: `${heldPct}%` }} className="h-full bg-amber-500 transition-all duration-500"></div>
          <div style={{ width: `${availablePct}%` }} className="h-full bg-emerald-500 transition-all duration-500"></div>
        </div>
      </div>

      {/* 2. Operations Controls Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* Card A: Initialize Venue Capacity */}
        <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-blue-500/20 text-blue-400">
              <RotateCcw className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">Initialize Venue Capacity</h3>
              <p className="text-[11px] text-slate-400">Resets inventory &amp; provisions $N$ available seats.</p>
            </div>
          </div>

          <div className="grid grid-cols-4 gap-1.5 pt-1">
            {[24, 50, 100, 250].map((count) => (
              <button
                key={count}
                onClick={() => setInitCount(count)}
                className={`py-1.5 rounded-lg text-xs font-semibold border ${
                  initCount === count
                    ? "bg-blue-600 border-blue-500 text-white"
                    : "bg-slate-900 border-white/5 text-slate-300 hover:bg-slate-800"
                }`}
              >
                {count} Seats
              </button>
            ))}
          </div>

          <div className="flex gap-2">
            <input
              type="number"
              value={initCount}
              onChange={(e) => setInitCount(parseInt(e.target.value, 10) || 50)}
              className="w-24 px-3 py-2 bg-slate-900 border border-white/10 rounded-xl text-xs text-white"
            />
            <button
              onClick={handleInitVenue}
              className="flex-1 py-2 rounded-xl bg-red-600/80 hover:bg-red-600 text-white text-xs font-bold transition-colors"
            >
              Reset Venue
            </button>
          </div>
        </div>

        {/* Card B: Dynamic Capacity Expansion */}
        <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-500/20 text-emerald-400">
              <PlusCircle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">Dynamic Live Expansion</h3>
              <p className="text-[11px] text-slate-400">Adds $N$ seats. Auto-fulfills waiting customers.</p>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-1.5 pt-1">
            {[5, 10, 25].map((count) => (
              <button
                key={count}
                onClick={() => setExpandCount(count)}
                className={`py-1.5 rounded-lg text-xs font-semibold border ${
                  expandCount === count
                    ? "bg-emerald-600 border-emerald-500 text-white"
                    : "bg-slate-900 border-white/5 text-slate-300 hover:bg-slate-800"
                }`}
              >
                +{count} Seats
              </button>
            ))}
          </div>

          <div className="flex gap-2">
            <input
              type="number"
              value={expandCount}
              onChange={(e) => setExpandCount(parseInt(e.target.value, 10) || 10)}
              className="w-24 px-3 py-2 bg-slate-900 border border-white/10 rounded-xl text-xs text-white"
            />
            <button
              onClick={handleExpandVenue}
              className="flex-1 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition-colors"
            >
              + Expand Capacity
            </button>
          </div>
        </div>

        {/* Card C: Flash-Sale Burst Simulator */}
        <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-amber-500/20 text-amber-400">
              <Flame className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white">Flash-Sale Burst Simulator</h3>
              <p className="text-[11px] text-slate-400">Fires 10 concurrent requests across virtual threads.</p>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-slate-900 text-[11px] text-slate-300 min-h-[40px] flex items-center">
            {burstStatus || "Ready to fire 10 simultaneous reservations."}
          </div>

          <button
            onClick={handleBurstSimulator}
            disabled={isBursting}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-amber-600 to-amber-700 hover:from-amber-500 hover:to-amber-600 text-white text-xs font-bold flex items-center justify-center gap-2 shadow-lg shadow-amber-900/30"
          >
            <Zap className="w-4 h-4" />
            <span>{isBursting ? "Firing Burst..." : "🔥 Trigger Concurrency Burst"}</span>
          </button>
        </div>
      </div>

      {/* 3. Live Priority Waitlist Management Table */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/10 space-y-4">
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-2">
            <Users className="w-5 h-5 text-purple-400" />
            <h3 className="text-base font-bold text-white">Live Priority Waitlist Queue</h3>
          </div>
          <span className="text-xs text-slate-400">
            {waitlist.length} Users Waiting
          </span>
        </div>

        {waitlist.length === 0 ? (
          <div className="py-8 text-center text-slate-400 text-xs">
            No customers currently waiting in the queue.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="text-slate-400 border-b border-white/5 pb-2">
                  <th className="py-3 px-4 font-semibold">Queue Pos</th>
                  <th className="py-3 px-4 font-semibold">Customer Handle</th>
                  <th className="py-3 px-4 font-semibold">Priority Tier</th>
                  <th className="py-3 px-4 font-semibold">Joined At</th>
                  <th className="py-3 px-4 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {waitlist.map((entry) => (
                  <tr key={entry.userId} className="hover:bg-white/5 transition-colors">
                    <td className="py-3 px-4 font-bold text-blue-400">#{entry.queuePosition}</td>
                    <td className="py-3 px-4 font-semibold text-white">{entry.userId}</td>
                    <td className="py-3 px-4">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                        Tier {entry.priority}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-400">{new Date(entry.timestamp).toLocaleTimeString()}</td>
                    <td className="py-3 px-4 text-right space-x-2">
                      <button
                        onClick={() => handlePromoteUser(entry.userId, entry.priority)}
                        className="px-2.5 py-1 rounded-lg bg-blue-600/20 text-blue-300 border border-blue-500/30 text-[10px] font-semibold hover:bg-blue-600/30"
                      >
                        Promote (+1)
                      </button>
                      <button
                        onClick={() => handleRemoveUser(entry.userId)}
                        className="px-2.5 py-1 rounded-lg bg-red-600/20 text-red-300 border border-red-500/30 text-[10px] font-semibold hover:bg-red-600/30"
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
