import service from "@/commons/base/service";
import { get } from "http";
import { useCallback, useEffect, useRef, useState } from "react";
import { ColumnDefinition } from "../workbenches/design/types";
import { Button, Card, Col, Form, Input, Row, Space, Timeline, Typography } from "antd";
import { UserOutlined } from "@ant-design/icons";
import { FlowForm } from "./types";


interface Props {
    workbenchId: string;
    formId: number;


}
export default function FormDetail({ workbenchId, formId }: Props) {
    const [columns, setColumns] = useState<ColumnDefinition[]>([]);
    const [flowForm, setFlowForm] = useState<FlowForm>();
    const [buttons, setButtons] = useState<any>(null);
    const loadingRef = useRef(false);
    const getFormDetail = useCallback(() => {
        if (workbenchId && formId && !loadingRef.current) {
            loadingRef.current = true;
            service.get(`/flowForm/${workbenchId}/getFlowFormDetail/${formId}`).then((res) => {
                setFlowForm(res.data.flowForm);
                setButtons(res.data.buttons);
                setColumns(res.data.columns);
            }).finally(() => {
                loadingRef.current = false;
            })
        }
    }, [workbenchId, formId]);

    useEffect(() => {
        getFormDetail();

    }, [getFormDetail]);
    const [form] = Form.useForm();
    const sunbmit = (flowId:number) => { 
        form.setFieldValue("flowId", flowId);
        console.log(form.getFieldsValue());
        service.post(`/workflow/${workbenchId}/approval`, form.getFieldsValue()).then((res) => {
            
        })
    };
    return (
        <div>
            <Row gutter={16}>

                {columns.map((column) => {
                    return (
                        <Col span={12} key={column.columnName}>
                            <Space>
                                <Typography.Text strong>{column.label}:</Typography.Text>
                                <Typography.Text>{flowForm?.data[column.columnName]}{column.unit}</Typography.Text>
                            </Space>
                        </Col>
                    )
                })}
            </Row>
            {flowForm && flowForm.flowHistories && flowForm.flowHistories.length > 0 && (
                <Card title={<Typography.Title level={4}>流转历史</Typography.Title>} style={{ marginBottom: '24px' }}>
                    <Timeline items={flowForm.flowHistories.map((history, index) => ({
                        dot: <UserOutlined style={{ fontSize: '16px' }} />,
                        children: <><Space direction="vertical" size={0}>
                            <Typography.Text strong>{history.operatorName}</Typography.Text>
                            <Typography.Text>{history.flowName}</Typography.Text>
                            <Typography.Text type="secondary">{history.createAt}</Typography.Text>
                        </Space>
                            <Typography.Text type="secondary">{history.approvalOpinion}</Typography.Text>
                        </>,
                        key: history.id,

                    }))} />
                </Card>
            )}
            {buttons && (
                <Form form={form}>
                    <Card>
                        <Form.Item name={"approvalOpinion"} label="审批意见"> 
                            <Input.TextArea rows={4} placeholder="请输入审批意见" />
                        </Form.Item>
                        <Form.Item hidden name={"flowId"}>
                            <Input/>
                        </Form.Item>
                        <Form.Item hidden name={"flowFormId"} initialValue={flowForm?.id}>
                            <Input/>
                        </Form.Item>
                        <Row justify="end" gutter={16}>
                            {buttons.map((button:any, index:number) => {
                                return (
                                    <Col key={index}>
                                        <Button type="primary" onClick={() => {sunbmit(button.value)}}>{button.name}</Button>
                                    </Col>
                                )
                            })}
                        </Row>
                    </Card>
                </Form>
            )}
        </div>
    )
}