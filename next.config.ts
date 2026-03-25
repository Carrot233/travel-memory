import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  allowedDevOrigins: ['106.12.26.246', 'localhost'],
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
