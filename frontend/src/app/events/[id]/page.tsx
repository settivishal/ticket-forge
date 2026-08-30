"use client";

import React, { useState, useEffect, use } from "react";
import Link from "next/link";
import { FEATURED_EVENTS } from "@/lib/events-data";
import { useAuth } from "@/lib/auth-context";
import { fetchSeats, fetchSystemStatus, reserveSeat, holdSeatGraphQL } from "@/lib/api";
import { SeatItem, SystemStatus, EventItem, DomainEvent } from "@/lib/types";
import confetti from "canvas-confetti";
import {
  Calendar,
  MapPin,
  Clock,
  Ticket,
  Sparkles,
  RefreshCw,
  Shield,
  Zap,
  Timer,
  CheckCircle2,
  X,
  AlertCircle,
  ArrowRight,
} from "lucide-react";

export default function EventBookingPage({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = use(params);
  const { currentUser, token, openAuthModal } = useAuth();

  const event: EventItem =
    FEATURED_EVENTS.find((e) => e.id === resolvedParams.id) || FEATURED_EVENTS[0];

  const [seats, setSeats] = useState<SeatItem[]>([]);
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null);
  const [selectedSeat, setSelectedSeat] = useState<SeatItem | null>(null);
  const [isReserving, setIsReserving] = useState(false);
  const [isHolding, setIsHolding] = useState(false);

  // Hold Timer state
  const [holdSecondsRemaining, setHoldSecondsRemaining] = useState<number>(0);

  // Order Confirmation Modal state
  const [confirmedReservation, setConfirmedReservation] = useState<{
    seatNumber: number;
    tier: string;
    price: number;
  } | null>(null);

  // Toast notifications state
  const [toastMsg, setToastMsg] = useState<{ text: string; type: "success" | "warning" | "info" } | null>(null);

  const showToast = (text: string, type: "success" | "warning" | "info" = "info") => {
    setToastMsg({ text, type });
    setTimeout(() => setToastMsg(null), 4000);
  };

  // Load seats and system telemetry
  const loadData = async () => {
    const [sData, stData] = await Promise.all([fetchSeats(token, currentUser), fetchSystemStatus()]);
    setSeats(sData);
    setSystemStatus(stData);
  };

  useEffect(() => {
    loadData();

    // SSE EventSource for real-time live synchronization
    let eventSource: EventSource | null = null;
    try {
      eventSource = new EventSource("/events/stream");

      eventSource.addEventListener("DOMAIN_EVENT", (e) => {
        try {
          const domEvent: DomainEvent = JSON.parse(e.data);
          if (domEvent.eventType === "SEAT_RESERVED") {
            showToast(`Seat #${domEvent.seatNumber} reserved (${domEvent.userId})`, "success");
          } else if (domEvent.eventType === "SEAT_HELD") {
            showToast(`Seat #${domEvent.seatNumber} placed on active hold`, "warning");
          } else if (domEvent.eventType === "VENUE_EXPANDED") {
            showToast(`Venue capacity expanded!`, "info");
          }
          loadData();
        } catch (err) {
          console.error("SSE parse error:", err);
        }
      });
    } catch (err) {
      console.warn("SSE connection warning:", err);
    }

    return () => {
      if (eventSource) eventSource.close();
    };
  }, [token, currentUser]);

  // Countdown timer effect
  useEffect(() => {
    if (holdSecondsRemaining <= 0) return;
    const interval = setInterval(() => {
      setHoldSecondsRemaining((prev) => {
        if (prev <= 1) {
          showToast("⏱️ Hold expired! Seat released back to inventory.", "info");
          loadData();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [holdSecondsRemaining]);

  const getSeatTierDetails = (seatNumber: number, tierName?: string) => {
    if (tierName === "VIP" || seatNumber <= 10) {
      return { name: "VIP Gold Circle", price: 250, fee: 15, tax: 8, color: "text-amber-400", bg: "bg-amber-500/20 border-amber-500/40" };
    } else if (tierName === "PREMIUM" || seatNumber <= 30) {
      return { name: "Premium Orchestra", price: 120, fee: 10, tax: 5, color: "text-blue-400", bg: "bg-blue-500/20 border-blue-500/40" };
    } else {
      return { name: "Standard Arena", price: 65, fee: 6, tax: 3, color: "text-emerald-400", bg: "bg-emerald-500/20 border-emerald-500/40" };
    }
  };

  const handleSeatClick = (seat: SeatItem) => {
    setSelectedSeat(seat);
  };

  const handleReserve = async () => {
    if (!currentUser) {
      openAuthModal("SIGN_IN");
      return;
    }

    setIsReserving(true);
    const userId = currentUser.id || currentUser.email;
    const priority = currentUser.priority || 1;

    const res = await reserveSeat(userId, priority, token, currentUser);
    setIsReserving(false);

    if (res.status === 201 && res.data) {
      const tierInfo = getSeatTierDetails(res.data.seatNumber, res.data.tier);
      setConfirmedReservation({
        seatNumber: res.data.seatNumber,
        tier: tierInfo.name,
        price: tierInfo.price + tierInfo.fee + tierInfo.tax,
      });
      setHoldSecondsRemaining(0);
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
      });
      showToast(`🎉 Seat #${res.data.seatNumber} reserved successfully!`, "success");
      loadData();
    } else if (res.status === 202) {
      showToast(`⏳ Venue sold out! Added ${userId} to Priority Waitlist.`, "warning");
      loadData();
    } else {
      showToast(res.message || "Reservation failed.", "warning");
    }
  };

  const handleHold = async () => {
    if (!currentUser) {
      openAuthModal("SIGN_IN");
      return;
    }

    setIsHolding(true);
    const userId = currentUser.id || currentUser.email;
    const priority = currentUser.priority || 1;

    const res = await holdSeatGraphQL(userId, priority, 60, token, currentUser);
    setIsHolding(false);

    if (res.success && res.seatNumber) {
      setHoldSecondsRemaining(60);
      showToast(`⏱️ Seat #${res.seatNumber} held for 60 seconds!`, "warning");
      loadData();
    } else {
      showToast(res.message || "Unable to hold seat.", "warning");
    }
  };

  const selectedTier = selectedSeat ? getSeatTierDetails(selectedSeat.seatNumber, selectedSeat.tier) : null;
  const totalPrice = selectedTier ? selectedTier.price + selectedTier.fee + selectedTier.tax : 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-8">
      {/* Toast Notification */}
      {toastMsg && (
        <div
          className={`fixed top-20 right-6 z-50 px-4 py-3 rounded-xl shadow-2xl border text-xs font-semibold backdrop-blur-md flex items-center gap-2 animate-slideIn ${
            toastMsg.type === "success"
              ? "bg-emerald-950/90 text-emerald-300 border-emerald-500/40"
              : toastMsg.type === "warning"
              ? "bg-amber-950/90 text-amber-300 border-amber-500/40"
              : "bg-blue-950/90 text-blue-300 border-blue-500/40"
          }`}
        >
          <Sparkles className="w-4 h-4" />
          <span>{toastMsg.text}</span>
        </div>
      )}

      {/* 1. Event Hero Banner */}
      <div className="relative rounded-3xl overflow-hidden glass-panel border border-white/10 p-6 sm:p-10 bg-gradient-to-r from-slate-900/90 via-slate-900/60 to-blue-950/40">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-[11px] font-bold uppercase tracking-wider bg-blue-500/20 text-blue-300 border border-blue-500/30">
              <Sparkles className="w-3.5 h-3.5 text-amber-400" />
              <span>Interactive 2D Amphitheater Seating</span>
            </div>
            <h1 className="text-2xl sm:text-4xl font-black text-white tracking-tight">
              {event.title}
            </h1>
            <div className="flex flex-wrap items-center gap-4 text-xs text-slate-300 pt-1">
              <span className="flex items-center gap-1.5">
                <Calendar className="w-4 h-4 text-blue-400" />
                {event.date}
              </span>
              <span className="flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-amber-400" />
                {event.time}
              </span>
              <span className="flex items-center gap-1.5">
                <MapPin className="w-4 h-4 text-emerald-400" />
                {event.venue}, {event.city}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={loadData}
              className="px-4 py-2.5 rounded-xl bg-slate-900 border border-white/10 hover:border-white/20 text-xs font-semibold text-slate-300 flex items-center gap-2 transition-all"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Sync Map</span>
            </button>
            <Link
              href="/wallet"
              className="px-4 py-2.5 rounded-xl btn-electric text-xs font-semibold flex items-center gap-2"
            >
              <Ticket className="w-3.5 h-3.5" />
              <span>My Passes</span>
            </Link>
          </div>
        </div>
      </div>

      {/* 2. Main Seating Arena Grid & Ticketmaster Checkout Drawer */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left 8 Cols: Interactive 2D Seating Chart */}
        <div className="lg:col-span-8 space-y-6">
          <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/10 space-y-6">
            <div className="flex items-center justify-between border-b border-white/10 pb-4">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <Ticket className="w-5 h-5 text-blue-400" />
                  <span>Select Your Seat in the Amphitheater</span>
                </h2>
                <p className="text-xs text-slate-400 mt-0.5">
                  Click on an available seat to inspect tier pricing and place an instant reservation or 60s hold.
                </p>
              </div>

              {/* Real-time Status Badge */}
              <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900 border border-white/10 text-xs text-slate-300">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                <span>{systemStatus?.availableSeats ?? 0} Seats Open</span>
              </div>
            </div>

            {/* Arched Stage Visual */}
            <div className="flex flex-col items-center justify-center pt-2">
              <div className="w-3/4 max-w-md py-2.5 px-6 rounded-t-full bg-gradient-to-b from-blue-600/30 via-slate-900 to-slate-950 border-t-2 border-blue-500 text-center shadow-lg shadow-blue-900/20">
                <span className="text-[11px] font-extrabold uppercase tracking-widest text-blue-400">
                  ★ MAIN STAGE ★
                </span>
              </div>
              <div className="w-full h-4 border-b border-dashed border-white/10 mb-6"></div>
            </div>

            {/* 2D Interactive Seating Matrix */}
            <div className="grid grid-cols-5 sm:grid-cols-8 md:grid-cols-10 gap-2.5 max-h-[420px] overflow-y-auto p-2">
              {seats.map((seat) => {
                const tierInfo = getSeatTierDetails(seat.seatNumber, seat.tier);
                const isSelected = selectedSeat?.seatNumber === seat.seatNumber;

                let statusColor = "bg-emerald-600/30 border-emerald-500/50 text-emerald-300 hover:border-emerald-400";
                if (seat.seatNumber <= 10) {
                  statusColor = "bg-amber-600/30 border-amber-500/50 text-amber-300 hover:border-amber-400";
                } else if (seat.seatNumber <= 30) {
                  statusColor = "bg-blue-600/30 border-blue-500/50 text-blue-300 hover:border-blue-400";
                }

                if (seat.status === "HELD") {
                  statusColor = "bg-amber-500/40 border-amber-400 text-amber-200 pulse-amber";
                } else if (seat.status === "RESERVED") {
                  statusColor = "bg-slate-800/80 border-slate-700/60 text-slate-500 cursor-not-allowed opacity-50";
                }

                return (
                  <button
                    key={seat.seatNumber}
                    onClick={() => handleSeatClick(seat)}
                    disabled={seat.status === "RESERVED"}
                    className={`relative p-2.5 rounded-xl border flex flex-col items-center justify-center transition-all ${statusColor} ${
                      isSelected ? "ring-2 ring-blue-400 ring-offset-2 ring-offset-slate-950 scale-105" : ""
                    }`}
                  >
                    <span className="text-[11px] font-bold">#{seat.seatNumber}</span>
                    <span className="text-[9px] uppercase font-semibold opacity-75">
                      {seat.status === "HELD" ? "HELD" : seat.status === "RESERVED" ? "SOLD" : `$${tierInfo.price}`}
                    </span>
                  </button>
                );
              })}
            </div>

            {/* Seating Legend */}
            <div className="flex flex-wrap items-center justify-center gap-6 pt-4 border-t border-white/10 text-xs text-slate-400">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-md bg-amber-500/40 border border-amber-400"></span>
                <span>VIP Gold ($250)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-md bg-blue-500/40 border border-blue-400"></span>
                <span>Premium ($120)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-md bg-emerald-500/40 border border-emerald-400"></span>
                <span>Standard ($65)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-md bg-amber-500 pulse-amber"></span>
                <span>Held (Active Timer)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded-md bg-slate-700"></span>
                <span>Sold Out</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right 4 Cols: Ticketmaster Itemized Checkout Drawer */}
        <div className="lg:col-span-4 space-y-6">
          <div className="glass-panel rounded-3xl p-6 border border-white/10 space-y-6 sticky top-24">
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Zap className="w-4 h-4 text-blue-400" />
                <span>Ticket Checkout Summary</span>
              </h3>
              <span className="text-xs text-slate-400">Instant Pass</span>
            </div>

            {selectedSeat && selectedTier ? (
              <div className="space-y-4">
                {/* Seat Detail Card */}
                <div className="p-4 rounded-2xl bg-slate-900/90 border border-white/10 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-lg font-black text-white">Seat #{selectedSeat.seatNumber}</span>
                    <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase ${selectedTier.bg} ${selectedTier.color}`}>
                      {selectedTier.name}
                    </span>
                  </div>
                  <div className="text-xs text-slate-400">
                    Status: <strong className="text-emerald-400">{selectedSeat.status}</strong>
                  </div>
                </div>

                {/* Itemized Price Breakdown */}
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between text-slate-300">
                    <span>Face Value Ticket</span>
                    <span>${selectedTier.price}.00</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>Facility Convenience Fee</span>
                    <span>${selectedTier.fee}.00</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>State / Local Service Tax</span>
                    <span>${selectedTier.tax}.00</span>
                  </div>
                  <div className="pt-2 border-t border-white/10 flex justify-between text-sm font-bold text-white">
                    <span>Total Due (Guaranteed)</span>
                    <span className="text-blue-400">${totalPrice}.00</span>
                  </div>
                </div>

                {/* Hold Countdown Timer */}
                {holdSecondsRemaining > 0 && (
                  <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Timer className="w-4 h-4 animate-spin text-amber-400" />
                      <span className="text-xs font-semibold">Seat Hold Active</span>
                    </div>
                    <span className="text-sm font-mono font-bold">
                      00:{String(holdSecondsRemaining).padStart(2, "0")}
                    </span>
                  </div>
                )}

                {/* Action Buttons */}
                <div className="space-y-2.5 pt-2">
                  <button
                    onClick={handleReserve}
                    disabled={isReserving}
                    className="w-full py-3.5 btn-electric rounded-xl font-bold text-sm flex items-center justify-center gap-2 shadow-lg shadow-blue-900/30"
                  >
                    <span>{isReserving ? "Processing Reservation..." : "⚡ Confirm & Reserve Seat"}</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>

                  <button
                    onClick={handleHold}
                    disabled={isHolding || holdSecondsRemaining > 0}
                    className="w-full py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-white/10 hover:border-amber-500/40 text-xs font-semibold text-slate-200 flex items-center justify-center gap-2 transition-all"
                  >
                    <Timer className="w-4 h-4 text-amber-400" />
                    <span>⏱️ Place 60s Hold (GraphQL)</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="py-10 text-center space-y-2 text-slate-400">
                <Ticket className="w-8 h-8 text-slate-600 mx-auto" />
                <p className="text-xs">No seat selected yet.</p>
                <p className="text-[11px] text-slate-500">Click an open seat in the arena map to view pricing.</p>
              </div>
            )}

            {/* Waiting Room Card (When Sold Out) */}
            <div className="p-4 rounded-2xl bg-slate-900/80 border border-white/10 space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-200">
                <Clock className="w-4 h-4 text-amber-400" />
                <span>High-Demand Priority Queue</span>
              </div>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                If seats sell out, your priority tier auto-secures the next released seat the instant a hold expires.
              </p>
              {currentUser && (
                <div className="text-[11px] text-slate-300 pt-1">
                  Your Tier: <strong className="text-amber-400">Tier {currentUser.priority} Access</strong>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 3. Order Confirmation Modal */}
      {confirmedReservation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
          <div className="relative w-full max-w-md overflow-hidden glass-panel rounded-3xl border border-white/15 shadow-2xl p-6 sm:p-8 space-y-6">
            <div className="text-center space-y-2">
              <div className="w-14 h-14 rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <h3 className="text-xl font-black text-white">Booking Confirmed!</h3>
              <p className="text-xs text-slate-300">
                Your admission ticket has been locked in the Red-Black Tree and issued to your wallet.
              </p>
            </div>

            {/* Digital Pass Preview */}
            <div className="p-5 rounded-2xl bg-gradient-to-br from-blue-950/80 to-slate-900 border border-blue-500/30 space-y-3">
              <div className="flex justify-between items-start">
                <div>
                  <span className="text-[10px] uppercase font-bold text-amber-400">Official Pass</span>
                  <h4 className="text-sm font-bold text-white">{event.title}</h4>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-300">
                  CONFIRMED
                </span>
              </div>

              <div className="pt-2 border-t border-white/10 flex justify-between items-center text-xs">
                <div>
                  <div className="text-base font-black text-white">Seat #{confirmedReservation.seatNumber}</div>
                  <div className="text-[11px] text-slate-400">{confirmedReservation.tier}</div>
                </div>
                <div className="text-right">
                  <div className="text-xs font-bold text-blue-400">${confirmedReservation.price}.00</div>
                  <div className="text-[10px] text-slate-400">Total Paid</div>
                </div>
              </div>
            </div>

            <div className="flex gap-3">
              <Link
                href="/wallet"
                className="flex-1 py-3 btn-electric rounded-xl text-center text-xs font-bold flex items-center justify-center gap-1.5"
              >
                <Ticket className="w-4 h-4" />
                <span>View My Digital Wallet</span>
              </Link>
              <button
                onClick={() => setConfirmedReservation(null)}
                className="px-4 py-3 rounded-xl bg-slate-900 border border-white/10 hover:bg-slate-800 text-xs font-semibold text-slate-300"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
