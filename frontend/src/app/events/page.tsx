"use client";

import React, { useState } from "react";
import Link from "next/link";
import { FEATURED_EVENTS } from "@/lib/events-data";
import { useAuth } from "@/lib/auth-context";
import { Calendar, MapPin, Search, ArrowRight, Sparkles, Filter, Ticket } from "lucide-react";

export default function EventsCatalogPage() {
  const { currentUser, openAuthModal } = useAuth();
  const [selectedCategory, setSelectedCategory] = useState<string>("All");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [sortBy, setSortBy] = useState<"PRICE_LOW" | "PRICE_HIGH" | "POPULAR">("POPULAR");

  const categories = ["All", "Concerts", "Sports", "Festivals", "VIP"];

  let events = FEATURED_EVENTS.filter((e) => {
    const matchesCat = selectedCategory === "All" || e.category === selectedCategory;
    const matchesSearch =
      e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.artist.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.venue.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.city.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

  if (sortBy === "PRICE_LOW") {
    events.sort((a, b) => a.minPrice - b.minPrice);
  } else if (sortBy === "PRICE_HIGH") {
    events.sort((a, b) => b.maxPrice - a.maxPrice);
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 w-full space-y-8">
      {/* Customer Loyalty Banner (When signed in) */}
      {currentUser && (
        <div className="glass-panel p-5 rounded-2xl border border-blue-500/30 bg-gradient-to-r from-blue-950/40 via-slate-900/60 to-slate-900/90 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600/30 border border-blue-500/40 text-blue-400">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-white">
                Welcome back, {currentUser.name}!
              </h2>
              <p className="text-xs text-slate-300">
                {currentUser.priority === 3
                  ? "⭐ Tier 3 VIP Early Access Active — Front Gold Circle & VIP Lounges Unlocked"
                  : currentUser.priority === 2
                  ? "💎 Tier 2 Presale Pass Active — 30-Minute Early Access"
                  : "🟢 Tier 1 Standard Fan Pass Active"}
              </p>
            </div>
          </div>

          <Link
            href="/wallet"
            className="px-4 py-2 text-xs font-bold btn-electric rounded-xl flex items-center gap-2 whitespace-nowrap"
          >
            <Ticket className="w-4 h-4" />
            <span>My Confirmed Passes</span>
          </Link>
        </div>
      )}

      {/* Page Header */}
      <div className="space-y-2">
        <h1 className="text-3xl font-black text-white tracking-tight">
          Explore Live Concerts, Tours &amp; Arenas
        </h1>
        <p className="text-sm text-slate-400">
          Select any live event below to access the interactive 2D Amphitheater Seating Arena and reserve your seats.
        </p>
      </div>

      {/* Search, Filter & Sort Controls */}
      <div className="glass-panel p-4 rounded-2xl border border-white/10 flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Search */}
        <div className="relative w-full md:w-96">
          <Search className="absolute left-3.5 top-3 w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search tours, artists, venues..."
            className="w-full pl-10 pr-3 py-2 bg-slate-900 border border-white/10 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Category Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                selectedCategory === cat
                  ? "bg-blue-600 text-white shadow-md"
                  : "bg-slate-900/80 text-slate-300 hover:text-white border border-white/5"
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* Sort Dropdown */}
        <div className="flex items-center gap-2 w-full md:w-auto">
          <Filter className="w-4 h-4 text-slate-400 shrink-0" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="w-full md:w-auto px-3 py-2 bg-slate-900 border border-white/10 rounded-xl text-xs text-white focus:outline-none focus:border-blue-500"
          >
            <option value="POPULAR">Most Popular</option>
            <option value="PRICE_LOW">Price: Low to High</option>
            <option value="PRICE_HIGH">Price: High to Low</option>
          </select>
        </div>
      </div>

      {/* Events Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {events.map((event) => (
          <div
            key={event.id}
            className="glass-panel rounded-2xl overflow-hidden border border-white/10 glass-card-hover flex flex-col group"
          >
            <div className="relative h-52 w-full overflow-hidden bg-slate-900">
              <img
                src={event.image}
                alt={event.title}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              />
              <div className="absolute top-3 left-3">
                <span className="px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase bg-black/75 text-blue-400 border border-blue-500/30 backdrop-blur-md">
                  {event.category}
                </span>
              </div>
              <div className="absolute bottom-3 right-3">
                <span className="px-3 py-1 rounded-lg text-xs font-bold bg-blue-600/90 text-white backdrop-blur-md">
                  ${event.minPrice} - ${event.maxPrice}
                </span>
              </div>
            </div>

            <div className="p-6 flex-1 flex flex-col justify-between space-y-4">
              <div>
                <div className="text-xs font-bold text-amber-400 uppercase tracking-wider mb-1">
                  {event.artist}
                </div>
                <h3 className="text-lg font-bold text-white leading-snug">
                  {event.title}
                </h3>
                <p className="text-xs text-slate-400 mt-1 line-clamp-2">
                  {event.subtitle}
                </p>

                <div className="space-y-1.5 mt-4 pt-4 border-t border-white/5 text-xs text-slate-300">
                  <div className="flex items-center gap-2">
                    <Calendar className="w-3.5 h-3.5 text-blue-400" />
                    <span>{event.date} &bull; {event.time}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <MapPin className="w-3.5 h-3.5 text-emerald-400" />
                    <span>{event.venue}, {event.city}</span>
                  </div>
                </div>

                {/* VIP Perks */}
                {event.vipPerks && (
                  <div className="mt-3 flex flex-wrap gap-1">
                    {event.vipPerks.slice(0, 2).map((perk, i) => (
                      <span key={i} className="px-2 py-0.5 rounded text-[10px] bg-amber-500/10 text-amber-300 border border-amber-500/20">
                        {perk}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              <div className="pt-4 border-t border-white/5 flex items-center justify-between">
                <span className="text-xs text-slate-400">
                  {event.totalSeats} Available Seats
                </span>
                <Link
                  href={`/events/${event.id}`}
                  className="px-4 py-2 text-xs font-bold btn-electric rounded-xl flex items-center gap-1.5 shadow-md shadow-blue-900/30"
                >
                  <span>Select Seats</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
