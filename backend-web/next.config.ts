import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async redirects() {
    return [
      {
        source: '/:path*.js',
        destination: '/:path*.js',
        permanent: true,
      },
      {
        source: '/:path*.ts',
        destination: '/:path*.ts',
        permanent: true,
      },
    ];
  },
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
