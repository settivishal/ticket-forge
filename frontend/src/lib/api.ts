import { SeatItem, SystemStatus, ReservationItem, WaitlistEntry, UserProfile } from "./types";

function getHeaders(token?: string | null, currentUser?: UserProfile | null, extraHeaders: HeadersInit = {}): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  } else if (currentUser) {
    if (currentUser.role === "ADMIN") {
      headers["Authorization"] = "Bearer dev-admin";
      headers["X-Dev-Role"] = "ADMIN";
      headers["X-Dev-User"] = currentUser.id;
    } else {
      headers["Authorization"] = "Bearer dev-customer";
      headers["X-Dev-Role"] = "CUSTOMER";
      headers["X-Dev-User"] = currentUser.id;
      headers["X-Dev-Priority"] = String(currentUser.priority || 1);
    }
  }

  return { ...headers, ...(extraHeaders as Record<string, string>) };
}

export async function fetchSystemStatus(): Promise<SystemStatus | null> {
  try {
    const res = await fetch("/api/v1/seats/availability", { cache: "no-store" });
    if (!res.ok) return null;
    const json = await res.json();
    return json.data;
  } catch (err) {
    console.error("Error fetching system status:", err);
    return null;
  }
}

export async function fetchSeats(token?: string | null, user?: UserProfile | null): Promise<SeatItem[]> {
  try {
    const res = await fetch("/api/v1/seats", {
      headers: getHeaders(token, user),
      cache: "no-store",
    });
    if (!res.ok) return [];
    const json = await res.json();
    return json.data || [];
  } catch (err) {
    console.error("Error fetching seats:", err);
    return [];
  }
}

export async function reserveSeat(
  userId: string,
  priority: number,
  token?: string | null,
  user?: UserProfile | null
): Promise<{ status: number; data?: ReservationItem; message?: string }> {
  try {
    const res = await fetch("/api/v1/reservations", {
      method: "POST",
      headers: getHeaders(token, user),
      body: JSON.stringify({ userId, priority }),
    });
    const json = await res.json();
    return {
      status: res.status,
      data: json.data,
      message: json.message || json.detail,
    };
  } catch (err) {
    console.error("Error reserving seat:", err);
    return { status: 500, message: "Network connection error." };
  }
}

export async function holdSeatGraphQL(
  userId: string,
  priority: number,
  ttlSeconds: number = 60,
  token?: string | null,
  user?: UserProfile | null
): Promise<{ success: boolean; seatNumber?: number; message?: string }> {
  const query = `
    mutation {
      holdSeat(userId: "${userId}", priority: ${priority}, ttlSeconds: ${ttlSeconds}) {
        seatNumber
        userId
        tier
        expiresAt
      }
    }
  `;

  try {
    const res = await fetch("/graphql", {
      method: "POST",
      headers: getHeaders(token, user),
      body: JSON.stringify({ query }),
    });
    const json = await res.json();
    if (json.data && json.data.holdSeat) {
      return { success: true, seatNumber: json.data.holdSeat.seatNumber };
    }
    return { success: false, message: json.errors?.[0]?.message || "Seat hold failed." };
  } catch (err) {
    console.error("Hold GraphQL error:", err);
    return { success: false, message: "Unable to communicate with GraphQL server." };
  }
}

export async function fetchMyReservations(
  token?: string | null,
  user?: UserProfile | null
): Promise<ReservationItem[]> {
  try {
    const res = await fetch("/api/v1/reservations", {
      headers: getHeaders(token, user),
      cache: "no-store",
    });
    if (!res.ok) return [];
    const json = await res.json();
    return json.data || [];
  } catch (err) {
    console.error("Error fetching reservations:", err);
    return [];
  }
}

export async function cancelReservation(
  seatNumber: number,
  userId: string,
  token?: string | null,
  user?: UserProfile | null
): Promise<boolean> {
  try {
    const res = await fetch(`/api/v1/reservations/${seatNumber}?userId=${encodeURIComponent(userId)}`, {
      method: "DELETE",
      headers: getHeaders(token, user),
    });
    return res.ok;
  } catch (err) {
    console.error("Cancellation error:", err);
    return false;
  }
}

export async function adminInitializeVenue(
  seatCount: number,
  token?: string | null,
  user?: UserProfile | null
): Promise<boolean> {
  try {
    const res = await fetch("/api/v1/seats/initialize", {
      method: "POST",
      headers: getHeaders(token, user),
      body: JSON.stringify({ seatCount }),
    });
    return res.ok;
  } catch (err) {
    console.error("Admin init error:", err);
    return false;
  }
}

export async function adminExpandVenue(
  additionalCount: number,
  token?: string | null,
  user?: UserProfile | null
): Promise<boolean> {
  try {
    const res = await fetch("/api/v1/seats/expand", {
      method: "POST",
      headers: getHeaders(token, user),
      body: JSON.stringify({ additionalCount }),
    });
    return res.ok;
  } catch (err) {
    console.error("Admin expand error:", err);
    return false;
  }
}

export async function adminReleaseRange(
  fromUserId: string,
  toUserId: string,
  token?: string | null,
  user?: UserProfile | null
): Promise<{ success: boolean; count: number }> {
  try {
    const res = await fetch("/api/v1/reservations/release-range", {
      method: "POST",
      headers: getHeaders(token, user),
      body: JSON.stringify({ fromUserId, toUserId }),
    });
    const json = await res.json();
    return { success: res.ok, count: json.data?.length || 0 };
  } catch (err) {
    console.error("Admin release range error:", err);
    return { success: false, count: 0 };
  }
}

export async function fetchAdminWaitlist(
  token?: string | null,
  user?: UserProfile | null
): Promise<WaitlistEntry[]> {
  try {
    const res = await fetch("/api/v1/waitlist", {
      headers: getHeaders(token, user),
      cache: "no-store",
    });
    if (!res.ok) return [];
    const json = await res.json();
    return json.data || [];
  } catch (err) {
    console.error("Waitlist fetch error:", err);
    return [];
  }
}

export async function promoteWaitlistUser(
  userId: string,
  newPriority: number,
  token?: string | null,
  user?: UserProfile | null
): Promise<boolean> {
  try {
    const res = await fetch(`/api/v1/waitlist/${userId}`, {
      method: "PATCH",
      headers: getHeaders(token, user),
      body: JSON.stringify({ newPriority }),
    });
    return res.ok;
  } catch (err) {
    console.error(err);
    return false;
  }
}

export async function removeWaitlistUser(
  userId: string,
  token?: string | null,
  user?: UserProfile | null
): Promise<boolean> {
  try {
    const res = await fetch(`/api/v1/waitlist/${userId}`, {
      method: "DELETE",
      headers: getHeaders(token, user),
    });
    return res.ok;
  } catch (err) {
    console.error(err);
    return false;
  }
}

export async function fetchAuthConfig(): Promise<{
  isDev: boolean;
  supabaseUrl: string;
  supabaseAnonKey: string;
}> {
  try {
    const res = await fetch("/api/v1/auth/config");
    if (res.ok) {
      const json = await res.json();
      return json.data || { isDev: true, supabaseUrl: "", supabaseAnonKey: "" };
    }
  } catch (err) {
    console.warn("Auth config fetch error:", err);
  }
  return { isDev: true, supabaseUrl: "", supabaseAnonKey: "" };
}
