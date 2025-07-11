"use client";
// import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import "antd/dist/reset.css"
import { Layout, message } from "antd";
import React from "react";
import { createCache, extractStyle, StyleProvider } from '@ant-design/cssinjs';
import type { DocumentContext } from 'next/document';
import Login from "./login/Login";
import SideMenu from "./login/Menu";

const { Header, Sider, Content, Footer } = Layout;

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});



function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const [messageApi, contextHolder] = message.useMessage();
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        {contextHolder}
        <Layout style={{ minHeight: "100vh" }}>
          <Header>
            <Login />
          </Header>

          <Layout>
            <Sider width={200} style={{ background: "#fff" }}>
              {/* 左侧菜单内容 */}
              <SideMenu />
            </Sider>

            <Content style={{ margin: "24px 16px 0" }}>
              {/* 主体内容区域 */}
              <div style={{ padding: 24, background: "#fff", minHeight: 360 }}>
                {children}
              </div>
            </Content>
          </Layout>

          <Footer style={{ textAlign: "center" }}>
            {/* 底部内容 */}
            <p>Footer</p>
          </Footer>
        </Layout>
      </body>
    </html>
  );
}

RootLayout.getInitialProps = async (ctx: DocumentContext) => {
  const cache = createCache();
  const originalRenderPage = ctx.renderPage;
  ctx.renderPage = () =>
    originalRenderPage({
      enhanceApp: (App) => (props) => (
        <StyleProvider cache={cache}>
          <App {...props} />
        </StyleProvider>
      ),
    });

  const initialProps = await Document.getInitialProps(ctx);
  const style = extractStyle(cache, true);
  return {
    ...initialProps,
    styles: (
      <>
        {initialProps.styles}
        <style dangerouslySetInnerHTML={{ __html: style }} />
      </>
    ),
  };
};

export default RootLayout;
