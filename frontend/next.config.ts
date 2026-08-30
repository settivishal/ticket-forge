import type { NextConfig } from "next";

const BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${BACKEND_URL}/api/:path*`,
      },
      {
        source: "/events/stream",
        destination: `${BACKEND_URL}/api/v1/events/stream`,
      },
      {
        source: "/graphql",
        destination: `${BACKEND_URL}/graphql`,
      },
      {
        source: "/actuator/:path*",
        destination: `${BACKEND_URL}/actuator/:path*`,
      },
    ];
  },
};

export default nextConfig;
