import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  async rewrites() {
    return [
      {
        source: "/user/:path*", // 
        destination: "http://127.0.0.1:8080/user/:path*", // 转发到你的后端服务
      },
      {
        source: "/permission/:path*", // 
        destination: "http://127.0.0.1:8080/permission/:path*", // 转发到你的后端服务
      },
    ];
  },
};

export default nextConfig;
