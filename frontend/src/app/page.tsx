"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { FEATURED_EVENTS } from "@/lib/events-data";
import { useAuth } from "@/lib/auth-context";
import { fetchSystemStatus } from "@/lib/api";
import { SystemStatus } from "@/lib/types";
import {
  Sparkles,
  Calendar,
  MapPin,
  Ticket,
  Search,
  Zap,
  ArrowRight,
  ShieldCheck,
  Cpu,
  Clock,
  Flame,
  CheckCircle2,
} from "lucide-react";

export default function LandingPage() {
  const { openAuthModal, currentUser } = useAuth();
  const [selectedCategory, setSelectedCategory] = useState<string>("All");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null);

  useEffect(() => {
    fetchSystemStatus().then(setSystemStatus);
    const interval = setInterval(() => {
      fetchSystemStatus().then(setSystemStatus);
    }, 10000);
    return () => clearInterval(interval);
  }, []);

  const categories = ["All", "Concerts", "Sports", "Festivals", "VIP"];

  const filteredEvents = FEATURED_EVENTS.filter((e) => {
    const matchesCat = selectedCategory === "All" || e.category === selectedCategory;
    const matchesSearch =
      e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.artist.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.venue.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.city.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

  const featured = FEATURED_EVENTS[0]; // Cyber Symphony 2026

  return (
    <div className="flex-1 flex flex-col space-y-16 pb-20">
      {/* 1. Hero Showcase (Ticketmaster Electric Blue Aesthetic) */}
      <section className="relative w-full min-h-[580px] flex items-center justify-center overflow-hidden border-b border-white/10">
        {/* Background Banner Image with Gradient Mask */}
        <div
          className="absolute inset-0 bg-cover bg-center opacity-30 transform scale-105 transition-transform duration-1000"
          style={{ backgroundImage: `url(${featured.bannerImage})` }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#060913] via-[#060913]/80 to-blue-950/40" />

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 flex flex-col items-center text-center space-y-6">
          {/* Live Flash-Sale Tag */}
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-blue-500/20 border border-blue-400/30 text-blue-300 text-xs font-semibold backdrop-blur-md animate-fadeIn">
            <span className="flex h-2 w-2 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
            </span>
            <span>⚡ OFFICIAL FLASH SALE &bull; LIVE 2D SEATING SELECTION</span>
          </div>

          <h1 className="text-4xl sm:text-6xl md:text-7xl font-black text-white tracking-tight leading-tight max-w-4xl">
            {featured.title}
          </h1>

          <p className="text-base sm:text-xl text-slate-300 max-w-2xl font-normal">
            Experience ultra-low latency ticket reservations powered by Spring Boot 3.3 Virtual Threads, Red-Black Trees, and instant distributed locking.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-6 text-sm text-slate-300 pt-2">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4 text-blue-400" />
              <span>{featured.date}</span>
            </div>
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-amber-400" />
              <span>{featured.time}</span>
            </div>
            <div className="flex items-center gap-2">
              <MapPin className="w-4 h-4 text-emerald-400" />
              <span>{featured.venue}, {featured.city}</span>
            </div>
          </div>

          {/* Call to Action Buttons */}
          <div className="flex flex-col sm:flex-row items-center gap-4 pt-4">
            <Link
              href={`/events/${featured.id}`}
              className="w-full sm:w-auto px-8 py-4 btn-electric rounded-2xl font-bold text-base flex items-center justify-center gap-2.5 shadow-xl shadow-blue-600/30 group"
            >
              <Ticket className="w-5 h-5 group-hover:rotate-12 transition-transform" />
              <span>Select Seats &amp; Book Tickets</span>
              <ArrowRight className="w-4 h-4" />
            </Link>

            <Link
              href="/events"
              className="w-full sm:w-auto px-6 py-4 rounded-2xl glass-panel border border-white/10 hover:border-white/30 text-white font-semibold text-base transition-all hover:bg-white/5"
            >
              Browse All 2026 Tours
            </Link>
          </div>

          {/* Real-time Telemetry Snapshot */}
          {systemStatus && (
            <div className="pt-6 flex flex-wrap items-center justify-center gap-4 text-xs">
              <div className="px-3.5 py-1.5 rounded-xl bg-slate-900/80 border border-white/10 text-slate-300">
                Available Arena Seats: <strong className="text-emerald-400">{systemStatus.availableSeats}</strong>
              </div>
              <div className="px-3.5 py-1.5 rounded-xl bg-slate-900/80 border border-white/10 text-slate-300">
                Active Confirmed: <strong className="text-blue-400">{systemStatus.reservedSeats}</strong>
              </div>
              <div className="px-3.5 py-1.5 rounded-xl bg-slate-900/80 border border-white/10 text-slate-300">
                Priority Queue: <strong className="text-amber-400">{systemStatus.waitlistCount}</strong>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* 2. Global Search & Category Filters */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full">
        <div className="glass-panel p-4 sm:p-6 rounded-2xl border border-white/10 shadow-2xl space-y-4">
          <div className="flex flex-col md:flex-row items-center gap-4">
            {/* Search Input */}
            <div className="relative flex-1 w-full">
              <Search className="absolute left-4 top-3.5 w-5 h-5 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by artist, tour, concert, city, or venue..."
                className="w-full pl-12 pr-4 py-3 bg-slate-900/90 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
              />
            </div>

            {/* Category Filter Pills */}
            <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
              {categories.map((cat) => (
                <button
                  key={cat}
                  onClick={() => setSelectedCategory(cat)}
                  className={`px-4 py-2.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap ${
                    selectedCategory === cat
                      ? "bg-blue-600 text-white shadow-lg shadow-blue-900/40"
                      : "bg-slate-900/80 text-slate-300 hover:text-white hover:bg-slate-800 border border-white/5"
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* 3. Featured & Trending Events Grid */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight flex items-center gap-2">
              <Flame className="w-6 h-6 text-amber-500" />
              <span>Trending Live Events &amp; Headline Tours</span>
            </h2>
            <p className="text-xs sm:text-sm text-slate-400 mt-1">
              Select an event to view real-time seating availability and reserve your admission passes.
            </p>
          </div>
          <Link
            href="/events"
            className="text-xs sm:text-sm font-semibold text-blue-400 hover:text-blue-300 flex items-center gap-1 group"
          >
            <span>View All</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {filteredEvents.map((event) => (
            <div
              key={event.id}
              className="glass-panel rounded-2xl overflow-hidden border border-white/10 glass-card-hover flex flex-col group"
            >
              {/* Event Card Image */}
              <div className="relative h-48 w-full overflow-hidden bg-slate-900">
                <img
                  src={event.image}
                  alt={event.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
                <div className="absolute top-3 left-3">
                  <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase tracking-wider bg-black/70 text-blue-400 border border-blue-500/30 backdrop-blur-md">
                    {event.category}
                  </span>
                </div>
                <div className="absolute bottom-3 right-3">
                  <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-blue-600/90 text-white backdrop-blur-md">
                    From ${event.minPrice}
                  </span>
                </div>
              </div>

              {/* Event Card Content */}
              <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                <div>
                  <div className="text-[11px] font-bold text-amber-400 uppercase tracking-wider mb-1">
                    {event.artist}
                  </div>
                  <h3 className="text-base font-bold text-white leading-snug line-clamp-2">
                    {event.title}
                  </h3>
                  <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-2">
                    <Calendar className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                    <span className="truncate">{event.date}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-1">
                    <MapPin className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span className="truncate">{event.venue}, {event.city}</span>
                  </div>
                </div>

                <div className="pt-3 border-t border-white/5 flex items-center justify-between">
                  <span className="text-[11px] text-slate-400">
                    Max: <strong className="text-slate-200">${event.maxPrice}</strong>
                  </span>
                  <Link
                    href={`/events/${event.id}`}
                    className="px-3.5 py-2 text-xs font-bold btn-electric rounded-xl flex items-center gap-1"
                  >
                    <span>Find Tickets</span>
                    <ArrowRight className="w-3 h-3" />
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 4. Concurrency Architecture & Engine Proof */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full">
        <div className="glass-panel rounded-3xl p-8 sm:p-12 border border-white/10 bg-gradient-to-b from-slate-900/90 via-slate-900/60 to-blue-950/40 space-y-8">
          <div className="text-center max-w-3xl mx-auto space-y-3">
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-500/20 text-blue-400 border border-blue-400/30 uppercase tracking-wider">
              High-Concurrency Architecture
            </span>
            <h2 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
              Engineered for Million-User Flash Sales
            </h2>
            <p className="text-slate-300 text-sm sm:text-base">
              Unlike legacy architectures that collapse under high traffic, TicketForge guarantees zero double-booking and sub-10ms response times.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-6 rounded-2xl bg-slate-900/90 border border-white/10 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-blue-500/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
                <Cpu className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white">Java 21 Virtual Threads (Loom)</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Spawns millions of lightweight virtual threads per request without thread exhaustion, achieving up to 10,000 requests per second.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900/90 border border-white/10 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white">Distributed Red-Black Trees</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Guarantees O(log N) optimal seat selection with zero race conditions, backed by Redisson distributed locks and Redis caching.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900/90 border border-white/10 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-amber-500/20 border border-amber-500/30 flex items-center justify-center text-amber-400">
                <Zap className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-white">Min-Heap Priority Waitlist</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Automatically auto-allocates released tickets to VIP and high-priority fans the millisecond seats are cancelled or expanded.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
