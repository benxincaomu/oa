"use client";
// import type { Metadata } from "next";
import "./globals.css";
import "antd/dist/reset.css"
function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
    return <>{children}</>
}

export default RootLayout;