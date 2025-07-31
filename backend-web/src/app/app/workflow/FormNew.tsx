
import service from "@/commons/base/service";
import { Button, Col, Form, Input, InputNumber, Row, Space, message } from "antd"
import { useCallback, useEffect, useState } from "react";
import { WorkbenchPublish } from "../workbenches/design/types";

interface Props {
    workbenchId?: string;
    formId?: number;
    workbenchPublish?: WorkbenchPublish;
    onSubmit?: () => void;
    onCancel?: () => void;
    disabled?:boolean;
}
/**
 * 
 *  表单的新增与编辑
 *  
 */
export default function FormEditor({ workbenchPublish, formId, onSubmit, onCancel,disabled=false }: Props) {
    const [messageApi, contextHolder] = message.useMessage();
    const [form] = Form.useForm();
    const getFlowForm = useCallback(() => {
        if (formId && workbenchPublish && workbenchPublish.workbenchId) {
            service.get(`/flowForm/${workbenchPublish.workbenchId}/${formId}`).then((res) => {
                if (res.data) {
                    form.setFieldsValue(res.data.data);
                    form.setFieldValue("id", res.data.id);
                }
            })
        }
    }, [form, formId, workbenchPublish]);


    useEffect(() => {

        getFlowForm();
        if(!formId){
            form.resetFields();
        }

    }, [form, formId, getFlowForm, workbenchPublish]);


    const onFinish = (values: any) => {
        if (!workbenchPublish) {

            console.log('preview:', values);
        } else {
            // console.log('new:', values);
            service.post(`/flowForm/${workbenchPublish.workbenchId}`, {
                publishId: workbenchPublish.id,
                data: values,
                id: values.id
            }).then(res => {
                if (res.code == 200) {
                    messageApi.success("提交成功");
                    if (onSubmit) {
                        onSubmit();
                    }
                }

            });
        }
    };

    const commitForm = () => {
        const values = form.getFieldsValue();
        if (!workbenchPublish) {

        } else {
            service.post(`/flowForm/${workbenchPublish.workbenchId}/commit`, {
                publishId: workbenchPublish.id,
                data: values,
                id: values.id
            }).then(res => {
                if (res.code == 200) {
                    messageApi.success("提交成功");
                    if (onSubmit) {
                        onSubmit();
                    }
                }

            });
        }
    };
    return (
        <div>
            {contextHolder}
            <Form form={form} onFinish={(values) => { onFinish(values) }} disabled = {disabled}>
                <Form.Item hidden name={"id"}>
                    <Input />
                </Form.Item>
                <Row gutter={16}>

                    {workbenchPublish?.entityDefinition.columns.map(column => {
                        return (
                            <Col key={column.columnName} span={column.columnType === "longtext" ? 24 : 12}>
                                <Form.Item label={column.label} name={column.columnName}>
                                    {(() => {
                                        switch (column.columnType) {
                                            case "string":
                                                return <Input />;
                                            case "number":
                                                return <InputNumber style={{ width: "100%" }} addonAfter={column.unit} />;
                                            default:
                                                return <></>;
                                        }
                                        return (
                                            <></>
                                        )
                                    })()}
                                </Form.Item>
                            </Col>
                        )
                    })
                    }
                </Row>
                <Row>
                    <Col span={16}>

                    </Col>
                    <Col span={8}>
                        <Space>
                            <Button type="primary" htmlType="submit">保存</Button>
                            <Button type="primary" onClick={() => { commitForm() }}>提交</Button>
                        </Space>
                    </Col>

                </Row>
            </Form>
        </div>
    )
}