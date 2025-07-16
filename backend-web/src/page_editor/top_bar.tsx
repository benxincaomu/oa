import { Button, Col, Form, Row, Switch } from "antd";
import React from "react";


export const TopBar = () => {
  return (
    <>
        <Row>
            <Col span={4}>
                <Form>
                    <Form.Item label="启用" initialValue={true}>
                        <Switch />启用
                    </Form.Item>
                </Form>
            </Col>
            <Col span={16}>
                
            </Col>
            <Col span={4}>
                <Button>打印JSON</Button>
            </Col>
        </Row>
    </>
  )
}