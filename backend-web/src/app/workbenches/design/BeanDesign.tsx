"use client"
import { Button, Col, Form, Input, Row, Select, Space, message } from "antd";
import { useEffect, useState } from "react";
import { ArrowUpOutlined, MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import service from "@/commons/base/service";
import { off } from "process";

type Props = {
    wid: number;
    setBeanForm: (beanForm: any) => void;
};

const BeanDesign = ({ wid,setBeanForm }: Props) => {
    const [messageApi, contextHolder] = message.useMessage();
    const [form] = Form.useForm();
    setBeanForm(form);
    const [id, setId] = useState(0);
    const onSave = (values: any) => {
        console.log("Received values of form: ", values);
        service.post("/entityDefinition", values).then(res => {
            messageApi.success("保存成功");
            setId(res.data);

        });
    };
    const [columnTypes, setColumnTypes] = useState<any[]>([]);

    useEffect(() => {
        service.get(`/entityDefinition/getColumnTypes`).then(res => {
            setColumnTypes(res.data);
        });
    });

    useEffect(() => {
        if (id > 0) {
            form.setFieldValue("id", id);
        }
    }, [id]);
    useEffect(() => {
        console.log("BeanDesign wid", wid);
        if (wid > 0) {
            service.get("/entityDefinition/getByWorkbenchId/" + wid).then(res => {
                if (res.data) {
                    form.setFieldsValue(res.data);
                }
            });
            form.setFieldValue("workbenchId", wid);
        }
    }, [wid]);
    const formListSpan = {
        labelCol: { span: 8 }, wrapperCol: { span: 18 }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', width: '100%' }}>
            {contextHolder}
            <Form form={form} layout="horizontal" labelCol={{ span: 3 }} style={{ maxWidth: "90%", minWidth: '1000px' }} onFinish={(values) => { onSave(values) }}>
                <Form.Item name={"workbenchId"} initialValue={wid} hidden>
                    <Input />
                </Form.Item>
                <Form.Item name={"id"} hidden>
                    <Input />
                </Form.Item>
                <Form.Item label="实体名称" name="entityName">
                    <Input placeholder="请输入实体名称" />
                </Form.Item>
                <Form.Item label="描述" name="entityDesc">
                    <Input.TextArea placeholder="请输入描述" />
                </Form.Item>

                <Form.List name="columns" >
                    {(fields, { add, remove ,move}) => (
                        <>
                            {fields.map((field, index) => (

                                <>
                                    <Row>
                                        <Col span={4}>
                                            <Form.Item name={[field.name, "sort"]} initialValue={index} hidden>
                                                <Input />
                                            </Form.Item>
                                            <Form.Item {...formListSpan} name={[field.name, "columnName"]} label="字段名">
                                                <Input />
                                            </Form.Item>
                                        </Col>
                                        <Col span={4}>
                                            <Form.Item {...formListSpan} name={[field.name, "label"]} label="展示名">
                                                <Input />
                                            </Form.Item>
                                        </Col>
                                        <Col span={5}>
                                            <Form.Item {...formListSpan} name={[field.name, "columnType"]} label="类型" style={{ minWidth: "220px" }}>
                                                <Select options={columnTypes} fieldNames={{ label: 'value', value: 'key' }} placeholder="请选择字段类型" style={{ minWidth: "150px" }} />
                                            </Form.Item>
                                        </Col>
                                        <Col span={4}>
                                            <Form.Item {...formListSpan} name={[field.name, "enumId"]} label="枚举">
                                                <Select>
                                                    <Select.Option value="">无</Select.Option>
                                                </Select>
                                            </Form.Item>
                                        </Col>
                                        <Col span={4}>
                                            <Form.Item {...formListSpan} name={[field.name, "validateTypes"]} label="校验规则">
                                                <Select mode="multiple">
                                                    <Select.Option value="">无</Select.Option>
                                                </Select>
                                            </Form.Item>
                                        </Col>
                                        <Col span={1}>
                                            <Form.Item {...formListSpan} style={{ width: '20px' }} wrapperCol={{ offset: 24 }}>
                                                <MinusCircleOutlined onClick={() => remove(index)} />
                                            </Form.Item>
                                        </Col>
                                            <Form.Item {...formListSpan} style={{ width: '20px' }} wrapperCol={{ offset: 24 }}>
                                                <ArrowUpOutlined onClick={() => move(index, index - 1)} />
                                            </Form.Item>
                                    </Row>
                                    {/* <span className="background-grey">字段{index + 1}</span> */}



                                </>

                            ))}
                            <Form.Item>
                                <Button type="dashed" onClick={() => add()} block >
                                    增加字段
                                </Button>
                            </Form.Item>
                        </>
                    )}
                </Form.List>

                <Form.Item >
                    <Button type="primary" htmlType="submit">
                        提交
                    </Button>
                </Form.Item>
            </Form>
        </div>
    );
};

export default BeanDesign;