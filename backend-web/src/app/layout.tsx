"use client";
import "./globals.css";
import { ConfigProvider } from 'antd';
export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <html lang="en">
    <body><ConfigProvider
      theme={{

        // algorithm: theme.defaultAlgorithm,
        components: {
          Table: {
            colorBorder: "#180bccff",
            colorBorderSecondary: "#7b799eff",
            rowSelectedHoverBg: "#f1f8f2ff",
            headerBg: "#7d9474ff",

          }
        }
      }}
    >{children}</ConfigProvider></body></html>
}
