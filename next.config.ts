import type { NextConfig } from "next";

const mediaHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Content-Security-Policy", value: "default-src 'none'; sandbox" },
];

const nextConfig: NextConfig = {
  allowedDevOrigins: ['106.12.26.246', 'localhost'],
  async rewrites() {
    return {
      beforeFiles: [
        {
          source: "/uploads/:path*",
          destination: "/api/uploads/:path*",
        },
      ],
    };
  },
  async headers() {
    return [
      { source: "/uploads/:path*", headers: mediaHeaders },
      { source: "/api/uploads/:path*", headers: mediaHeaders },
    ];
  },
  images: {
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '3000',
        pathname: '/api/uploads/**',
      },
      {
        protocol: 'http',
        hostname: '106.12.26.246',
        port: '3000',
        pathname: '/api/uploads/**',
      },
    ],
  },
};

export default nextConfig;
