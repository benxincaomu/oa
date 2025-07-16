import React from "react";
import { Button, Col, Row, Typography } from "antd";
import { TopBar } from "./top_bar";
import { Toolbox } from "./toolbox";
import { SettingPanel } from "./setting_panel";
import { UserButton, UserSpace, UserContainer,EditorText } from "./components";
import { Editor, Frame, Element } from "@craftjs/core";
const { Title, Text } = Typography;
export const PageEditor = () => {
    return (<>
        <Title level={5}>Editor</Title>
        <Editor resolver={{ UserButton, UserSpace, UserContainer ,EditorText}} enabled={true}>
            <Row style={{ backgroundColor: '#fff' }}>
                <Col span={24}>
                    <TopBar />
                </Col>
            </Row>
            <Row>

                <Col span={18} style={{ backgroundColor: '#dbc9c9ff' }}>
                    <Frame >
                        <Element is={UserContainer} canvas>

                            <UserSpace>
                                <Element is={UserContainer} canvas>
                                    <UserButton>按钮2</UserButton>
                                </Element>
                                <Element is={UserContainer} canvas>
                                    <UserButton>按钮3213</UserButton>
                                </Element>
                                <Element is={UserContainer} canvas>

                                    <UserButton>按钮</UserButton>
                                </Element>
                                {/* <UserButton>按钮321</UserButton> */}
                                <Element is={UserContainer} canvas>
                                    <EditorText text="asdsaf"/>
                                </Element>
                            </UserSpace>
                        </Element>
                    </Frame>
                    <div>


                    </div>
                </Col>
                <Col span={6}>
                    <Row><Toolbox /></Row>
                    <Row><div>&nbsp;</div></Row>
                    <Row><SettingPanel /></Row>

                </Col>
            </Row>
        </Editor>
    </>)
};