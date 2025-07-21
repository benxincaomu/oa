"use client"
import { Button, Checkbox, Col, Form, Input, Row, Select, Space, Table, message } from "antd";
import { useEffect, useState } from "react";
import { ArrowUpOutlined, MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import service from "@/commons/base/service";
import { off, title } from "process";

type Props = {
    wid: number;
    setBeanForm: (beanForm: any) => void;
};

const BeanDesign = ({ wid, setBeanForm }: Props) => {
    const [messageApi, contextHolder] = message.useMessage();
    const [form] = Form.useForm();
    setBeanForm(form);
    const [id, setId] = useState(0);
    const onSave = (values: any) => {
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
    }, []);

    useEffect(() => {
        if (id > 0) {
            form.setFieldValue("id", id);
        }
    }, [id]);
    useEffect(() => {
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
    const columns = [
        {
            title: '字段名称',
            dataIndex: 'columnName',
            key: 'columnName',
            render: (record: any, index: number) => {
                return <>
                </>
            },
        },
        {
            title: '字段类型',
            dataIndex: 'type',
            key: 'type',
        },
        {
            title: '字段长度',
            dataIndex: 'length',
            key: 'length',
        },
        {
            title: '字段描述',
            dataIndex: 'description',
            key: 'description',
        }

    ];

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

                <Row>
                    <Col className = "content-center" span={3}>
                        字段名
                    </Col>
                    <Col className = "content-center" span={3}>
                        展示名
                    </Col>
                    <Col className = "content-center" span={4}>
                        字段类型
                    </Col>
                    <Col className = "content-center" span={4}>
                        引用枚举
                    </Col>
                    <Col className = "content-center" span={4}>
                        校验规则
                    </Col>
                    <Col className = "content-center" span={2}>
                        列表展示
                    </Col>
                    <Col className = "content-center" span={3}>
                        操作
                    </Col>
                </Row>
                <Form.List name="columns" >
                    {(fields, { add, remove, move }) => (
                        <>
                            {fields.map((field, index) => (

                                    <Row key={index}>
                                        <Col className = "content-center" span={3}>
                                            <Form.Item name={[field.name, "sort"]} initialValue={index} hidden>
                                                <Input />
                                            </Form.Item>
                                            <Form.Item {...formListSpan} name={[field.name, "columnName"]} >
                                                <Input />
                                            </Form.Item>
                                        </Col>
                                        <Col className = "content-center" span={3}>
                                            <Form.Item {...formListSpan} name={[field.name, "label"]} >
                                                <Input />
                                            </Form.Item>
                                        </Col>
                                        <Col className = "content-center" span={4}>
                                            <Form.Item name={[field.name, "columnType"]}  >
                                                <Select options={columnTypes} fieldNames={{ label: 'value', value: 'key' }} placeholder="请选择字段类型" style={{ minWidth: "120px" }} />
                                            </Form.Item>
                                        </Col>
                                        <Col className = "content-center" span={4}>
                                            <Form.Item {...formListSpan} name={[field.name, "enumId"]} >
                                                <Select style={{ minWidth: "120px" }}>
                                                    <Select.Option value="" >无</Select.Option>
                                                </Select>
                                            </Form.Item>
                                        </Col>
                                        <Col className = "content-center" span={4}>
                                            <Form.Item {...formListSpan} name={[field.name, "validateTypes"]} >
                                                <Select mode="multiple" style={{ minWidth: "120px" }}>
                                                    <Select.Option value="">无</Select.Option>
                                                </Select>
                                            </Form.Item>
                                        </Col>
                                        <Col className = "content-center" span={2}>
                                            <Form.Item {...formListSpan} name={[field.name, "listAble"]} valuePropName="checked">
                                                <Checkbox />
                                            </Form.Item>
                                            
                                        </Col>
                                        <Col className = "content-center" span={3} >
                                                <MinusCircleOutlined onClick={() => remove(index)} />
                                                    &nbsp;&nbsp;
                                                <ArrowUpOutlined onClick={() => move(index, index - 1)} />
                                        </Col>
                                    </Row>
                                   




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