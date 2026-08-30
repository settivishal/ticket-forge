"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/auth-context";
import { fetchMyReservations, cancelReservation } from "@/lib/api";
import { ReservationItem } from "@/lib/types";
import {
  Ticket,
  QrCode,
  Calendar,
  MapPin,
  Sparkles,
  Trash2,
  ArrowRight,
  ShieldCheck,
  Smartphone,
  Share2,
} from "lucide-react";

export default function WalletPage() {
  const { currentUser, token, openAuthModal } = useAuth();
  const [tickets, setTickets] = useState<ReservationItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [cancelModalSeat, setCancelModalSeat] = useState<number | null>(null);

  const loadTickets = async () => {
    if (!currentUser) return;
    setIsLoading(true);
    const data = await fetchMyReservations(token, currentUser);
    const myId = currentUser.id || currentUser.email;
    const myTickets = data.filter((r) => r.userId === myId);
    setTickets(myTickets);
    setIsLoading(false);
  };

  useEffect(() => {
    if (currentUser) {
      loadTickets();
    } else {
      setIsLoading(false);
    }
  }, [currentUser, token]);

  const handleConfirmCancel = async () => {
    if (!cancelModalSeat || !currentUser) return;
    const userId = currentUser.id || currentUser.email;
    const ok = await cancelReservation(cancelModalSeat, userId, token, currentUser);
    setCancelModalSeat(null);
    if (ok) {
      loadTickets();
    }
  };

  if (!currentUser) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 text-center space-y-6">
        <div className="w-16 h-16 rounded-3xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center mx-auto">
          <Ticket className="w-8 h-8" />
        </div>
        <h1 className="text-3xl font-black text-white">Sign In to View Your Passes</h1>
        <p className="text-sm text-slate-400 max-w-md mx-auto">
          Your digital admission passes and reserved seats are tied to your TicketForge account.
        </p>
        <button
          onClick={() => openAuthModal("SIGN_IN")}
          className="px-6 py-3 btn-electric rounded-xl text-sm font-bold shadow-lg shadow-blue-900/30"
        >
          Sign In / Create Account
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 w-full space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-6">
        <div>
          <div className="flex items-center gap-2 text-xs font-bold text-blue-400 uppercase tracking-wider mb-1">
            <Smartphone className="w-4 h-4" />
            <span>Digital Ticket Wallet</span>
          </div>
          <h1 className="text-3xl font-black text-white tracking-tight">
            My Confirmed Admission Passes
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Showing verified digital tickets issued for <strong>{currentUser.name}</strong> ({currentUser.email}).
          </p>
        </div>

        <Link
          href="/events"
          className="px-4 py-2.5 btn-electric rounded-xl text-xs font-bold flex items-center gap-2 self-start sm:self-auto"
        >
          <Sparkles className="w-4 h-4 text-amber-300" />
          <span>Browse More Tours</span>
        </Link>
      </div>

      {/* Tickets List */}
      {isLoading ? (
        <div className="py-20 text-center text-slate-400 text-xs">
          Loading your verified passes...
        </div>
      ) : tickets.length === 0 ? (
        <div className="glass-panel rounded-3xl p-12 text-center border border-white/10 space-y-4 max-w-lg mx-auto">
          <Ticket className="w-12 h-12 text-slate-600 mx-auto" />
          <h3 className="text-lg font-bold text-white">No Tickets Reserved Yet</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            You don't have any confirmed bookings for this account. Pick your seats in the live 2D arena to secure admission.
          </p>
          <Link
            href="/events/cyber-symphony-2026"
            className="inline-flex items-center gap-2 px-5 py-2.5 btn-electric rounded-xl text-xs font-bold mt-2"
          >
            <span>Book Cyber Symphony 2026</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {tickets.map((ticket) => {
            const isVip = ticket.seatNumber <= 10;
            const isPremium = ticket.seatNumber > 10 && ticket.seatNumber <= 30;

            return (
              <div
                key={ticket.seatNumber}
                className="glass-panel rounded-3xl overflow-hidden border border-white/10 flex flex-col bg-gradient-to-b from-slate-900/90 via-slate-900/70 to-blue-950/30 shadow-2xl relative group"
              >
                {/* Pass Header */}
                <div className="p-5 border-b border-white/10 bg-gradient-to-r from-blue-900/40 to-slate-900/80 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-black tracking-widest text-amber-400 uppercase">
                      {isVip ? "⭐ VIP GOLD PASS" : isPremium ? "💎 PREMIUM PASS" : "🟢 STANDARD PASS"}
                    </span>
                    <h3 className="text-sm font-bold text-white">Cyber Symphony 2026</h3>
                  </div>
                  <div className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-[10px] font-bold">
                    <ShieldCheck className="w-3 h-3" />
                    <span>VERIFIED</span>
                  </div>
                </div>

                {/* Pass Body */}
                <div className="p-6 space-y-5 flex-1 flex flex-col justify-between">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="text-[10px] text-slate-400 uppercase font-bold">Assigned Seat</div>
                      <div className="text-3xl font-black text-white tracking-tight">
                        #{ticket.seatNumber}
                      </div>
                      <div className="text-xs text-slate-300">
                        {isVip ? "VIP Gold Circle (Row A)" : isPremium ? "Premium Center" : "Standard Arena"}
                      </div>
                    </div>

                    {/* QR Code Barcode Mockup */}
                    <div className="p-3 bg-white rounded-2xl shadow-md flex items-center justify-center">
                      <div className="w-16 h-16 bg-black flex items-center justify-center text-white font-mono text-[8px] leading-tight text-center p-1">
                        ■■■□■■<br />
                        ■□□■□■<br />
                        ■■■□■■<br />
                        □□■■□□<br />
                        ■■□■■■
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2 pt-4 border-t border-white/5 text-xs text-slate-300">
                    <div className="flex items-center gap-2">
                      <Calendar className="w-3.5 h-3.5 text-blue-400" />
                      <span>Saturday, Nov 14, 2026 &bull; 8:00 PM</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <MapPin className="w-3.5 h-3.5 text-emerald-400" />
                      <span>Neon Horizon Arena, San Francisco</span>
                    </div>
                  </div>

                  {/* Pass Footer Controls */}
                  <div className="pt-4 border-t border-white/10 flex items-center justify-between gap-3">
                    <span className="text-[10px] font-mono text-slate-500">
                      PASS-{ticket.seatNumber}099-TF
                    </span>
                    <button
                      onClick={() => setCancelModalSeat(ticket.seatNumber)}
                      className="px-3 py-1.5 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 text-xs font-semibold flex items-center gap-1.5 transition-colors"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      <span>Cancel Pass</span>
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Cancellation Confirmation Dialog */}
      {cancelModalSeat && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
          <div className="w-full max-w-sm glass-panel rounded-3xl p-6 border border-white/15 shadow-2xl space-y-4">
            <h3 className="text-base font-bold text-white">Cancel Admission Pass?</h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              Are you sure you want to release <strong>Seat #{cancelModalSeat}</strong> back to the public pool? If there is a priority waitlist queue, the seat will be immediately auto-allocated to the next waiting customer.
            </p>
            <div className="flex gap-3 pt-2">
              <button
                onClick={handleConfirmCancel}
                className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold transition-colors"
              >
                Yes, Release Seat
              </button>
              <button
                onClick={() => setCancelModalSeat(null)}
                className="px-4 py-2.5 rounded-xl bg-slate-900 border border-white/10 text-xs font-semibold text-slate-300"
              >
                Keep Pass
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
