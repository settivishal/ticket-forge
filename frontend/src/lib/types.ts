export type UserRole = "GUEST" | "CUSTOMER" | "ADMIN";

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  role: "CUSTOMER" | "ADMIN";
  priority: number; // 1: Standard, 2: Presale, 3: VIP
}

export interface EventItem {
  id: string;
  title: string;
  subtitle: string;
  artist: string;
  venue: string;
  city: string;
  date: string;
  time: string;
  category: "Concerts" | "Sports" | "Festivals" | "VIP";
  image: string;
  bannerImage: string;
  minPrice: number;
  maxPrice: number;
  vipPerks: string[];
  totalSeats: number;
  status: "ON_SALE" | "ALMOST_FULL" | "SOLD_OUT";
}

export interface SeatItem {
  id: number;
  seatNumber: number;
  status: "AVAILABLE" | "HELD" | "RESERVED";
  tier?: "VIP" | "PREMIUM" | "STANDARD";
  occupantUserId?: string;
}

export interface SystemStatus {
  totalSeats: number;
  availableSeats: number;
  reservedSeats: number;
  heldSeats: number;
  waitlistCount: number;
  engineMode: string;
  timestamp: string;
}

export interface ReservationItem {
  id: number;
  seatNumber: number;
  userId: string;
  tier?: string;
  reservedAt?: string;
  expiresAt?: string;
}

export interface WaitlistEntry {
  queuePosition: number;
  userId: string;
  priority: number;
  timestamp: string;
  status: string;
}

export interface DomainEvent {
  eventType: "SEAT_RESERVED" | "SEAT_HELD" | "SEAT_EXPIRED" | "RESERVATION_CANCELLED" | "VENUE_EXPANDED" | "SYSTEM";
  seatNumber?: number;
  userId?: string;
  message?: string;
  timestamp?: string;
}
