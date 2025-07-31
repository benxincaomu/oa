"use client";
// import type { Metadata } from "next";
import "./globals.css";
import { AntdRegistry } from '@ant-design/nextjs-registry';
import { ConfigProvider, theme } from 'antd';
function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <html lang="en">
    <body><ConfigProvider
      theme={{

        algorithm: theme.defaultAlgorithm,
        token: {
          // 边框颜色
          colorBorderSecondary:"#9b9b9bff",
          // controlItemBgHover:"#83c298ff",
        }
      }}
    >{children}</ConfigProvider></body></html>
}

export default RootLayout;