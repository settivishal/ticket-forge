import React from "react";
import Link from "next/link";
import { Ticket, Cpu, Activity, Database, Shield } from "lucide-react";

export function Footer() {
  return (
    <footer className="w-full border-t border-white/10 bg-slate-950/90 text-slate-400 text-xs py-10 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          {/* Brand Col */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-blue-600 text-white">
                <Ticket className="w-4 h-4" />
              </div>
              <span className="text-base font-bold text-white tracking-tight">TICKETFORGE</span>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed">
              Ultra-high concurrency flash-sale ticketing engine engineered on Spring Boot 3.3 Virtual Threads, Red-Black Trees, and Redis.
            </p>
          </div>

          {/* Engine Architecture */}
          <div>
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-3">Engine Tech Stack</h4>
            <ul className="space-y-2 text-xs">
              <li className="flex items-center gap-1.5"><Cpu className="w-3.5 h-3.5 text-blue-400" /> Java 21 Virtual Threads (Loom)</li>
              <li className="flex items-center gap-1.5"><Database className="w-3.5 h-3.5 text-emerald-400" /> Distributed Red-Black Trees</li>
              <li className="flex items-center gap-1.5"><Activity className="w-3.5 h-3.5 text-amber-400" /> Min-Heap Priority Waitlist</li>
              <li className="flex items-center gap-1.5"><Shield className="w-3.5 h-3.5 text-purple-400" /> Supabase OAuth2 / RS256 JWT</li>
            </ul>
          </div>

          {/* Developer Tools */}
          <div>
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-3">Live Developer Endpoints</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <a href="/swagger-ui.html" target="_blank" className="hover:text-blue-400 transition-colors">
                  📑 Swagger 3 OpenAPI Docs
                </a>
              </li>
              <li>
                <a href="/graphiql" target="_blank" className="hover:text-blue-400 transition-colors">
                  🔮 Interactive GraphiQL IDE
                </a>
              </li>
              <li>
                <a href="/actuator/health" target="_blank" className="hover:text-blue-400 transition-colors">
                  💓 Spring Actuator Health
                </a>
              </li>
              <li>
                <a href="/events/stream" target="_blank" className="hover:text-blue-400 transition-colors">
                  📡 SSE Domain Event Stream
                </a>
              </li>
            </ul>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-3">Quick Navigation</h4>
            <ul className="space-y-2 text-xs">
              <li><Link href="/events" className="hover:text-white transition-colors">Browse All Tours</Link></li>
              <li><Link href="/events/cyber-symphony-2026" className="hover:text-white transition-colors">Interactive 2D Seating Chart</Link></li>
              <li><Link href="/wallet" className="hover:text-white transition-colors">My Digital Wallet Passes</Link></li>
              <li><Link href="/admin" className="hover:text-amber-400 transition-colors">Admin Operations Command Center</Link></li>
            </ul>
          </div>
        </div>

        <div className="pt-6 border-t border-white/5 flex flex-col sm:flex-row items-center justify-between gap-4 text-[11px] text-slate-500">
          <div>
            TicketForge &copy; 2026. Inspired by Ticketmaster &amp; AXS. Built with Next.js 15 &amp; Spring Boot.
          </div>
          <div className="flex items-center gap-4">
            <span>Zero Double-Booking Guarantee</span>
            <span>&bull;</span>
            <span>Distributed Locking</span>
            <span>&bull;</span>
            <span>SSE Real-Time Sync</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
