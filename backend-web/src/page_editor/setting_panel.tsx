
import { Button, Col, Form, Row, Slider, Tag, Typography } from "antd";
import React from "react";

export const SettingPanel = () => {
    return <div className="width-100">
        <Row>
            <Col span={12}>
                <Typography.Title level={3}>选中</Typography.Title>
            </Col>
            <Col span={12}>
                <Tag color="grey">选中</Tag>
            </Col>
        </Row>
        <Row>
            <Col span={24}>
                <Form style={{width: "100%" }} >
                    <Form.Item label="属性" labelCol={{ span: 4 }} wrapperCol={{span: 16}}>
                        <Slider defaultValue={0} step={1} min={7} max={50} />
                    </Form.Item>
                </Form>
            </Col>
        </Row>
        <Button>删除</Button>

    </div>;
};