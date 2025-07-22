"use client";
// import type { Metadata } from "next";
import { Layout} from "antd";
import React from "react";
import Login from "@/app/login/Login";
import SideMenu from "@/app/login/Menu";

const { Header, Sider, Content, Footer } = Layout;


function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <Layout style={{ minHeight: "100vh",maxHeight: "100vh" }}>
          <Header>
            <Login />
          </Header>

          <Layout style={{height: "100%"}}>
            <Sider >
              {/* 左侧菜单内容 */}
              
              <SideMenu />
            </Sider>

            <Content style={{ margin: "24px 16px 0" }}>
              {/* 主体内容区域 */}
              <div className ="height-100" style={{ padding: 24,  minHeight: 360 }}>
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



export default RootLayout;
