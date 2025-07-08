import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  async rewrites() {
    return [
      {
        source: "/:path*", // 
        destination: "http://127.0.0.1:8080/:path*", // 转发到你的后端服务
      },
      
    ];
  },
};

export default nextConfig;
