import React from "react";
import { Button, Col, Row } from 'antd';
import { UserButton ,UserContainer} from "./components";
import { Element, useEditor } from "@craftjs/core";

export const Toolbox = () => {
    const { connectors, query } = useEditor();
  return <div>
    <div>ToolBox</div>
    <Row> 
        <Col span={12}>
          <Button ref={ref=> connectors.create(ref, <UserButton>按钮</UserButton>)}>按钮</Button>
        </Col>
        <Col span={12}>
          <Button ref={ref=> connectors.create(ref, <UserContainer>按钮</UserContainer>)}>容器</Button>
        </Col>
    </Row>
    <Row> 
        <Col span={12}>
          <Button>选择框</Button>
        </Col>
        <Col span={12}>
          <Button>Radio</Button>
        </Col>
    </Row>
    
  </div>;
};