"use client"
import { Button, Checkbox, Col, Form, Input, Modal, Row, Select, Space, Table, message } from "antd";
import { useEffect, useState } from "react";
import { ArrowUpOutlined, MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import service from "@/commons/base/service";
import { off, title } from "process";
import FormNew from "../../workflow/FormNew";

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
    const [columnTypeWatches, setColumnTypeWatches] = useState([]);
    useEffect(() => {
        service.get(`/entityDefinition/getColumnTypes`).then(res => {
            setColumnTypes(res.data);
        });
    }, [form]);

    useEffect(() => {
        if (id > 0) {
            form.setFieldValue("id", id);
        }
    }, [form, id]);
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
        labelCol: { span: 10 }, wrapperCol: { span: 14 }
    };

    const [previewFormVisitable, setPreviewFormVisitable] = useState(false);
    const previewForm = () => {
        setPreviewFormVisitable(true);
    }

    return (
        <div style={{ display: 'flex', justifyContent: 'center', width: '100%' }}>
            {contextHolder}
            <Form form={form} layout="horizontal" labelCol={{ span: 3 }} style={{ maxWidth: "90%", minWidth: '90%' }} onFinish={(values) => { onSave(values) }}>
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
                    {(fields, { add, remove, move }) => (
                        <>
                            {fields.map((field, index) => {
                                return (

                                    <Row key={index}>
                                        <Col className="content-center" span={3}>
                                            <Form.Item name={[field.name, "sort"]} initialValue={index} hidden>
                                                <Input />
                                            </Form.Item>
                                            <Form.Item {...formListSpan} name={[field.name, "columnName"]} label="字段名">
                                                <Input />
                                            </Form.Item>
                                        </Col>
                                        <Col className="content-center" span={3}>
                                            <Form.Item {...formListSpan} name={[field.name, "label"]} label="展示名">
                                                <Input />
                                            </Form.Item>
                                        </Col>
                                        <Col className="content-center" span={4}>
                                            <Form.Item {...formListSpan} name={[field.name, "columnType"]} label="类型">
                                                <Select options={columnTypes} fieldNames={{ label: 'value', value: 'key' }} placeholder="请选择字段类型" style={{ minWidth: "120px" }} />
                                            </Form.Item>
                                        </Col>
                                        <Form.Item noStyle shouldUpdate={(prevValues, currentValues) => {
                                            const prevType = prevValues.columns[index].columnType;
                                            const currentType = currentValues.columns[index].columnType;
                                            return prevType !== currentType;
                                        }}>
                                            {({ getFieldValue }) => {
                                                const columnType = getFieldValue(['columns', index, 'columnType']);
                                                if (columnType == 'number') {
                                                    return <Col className="content-center" span={4}>
                                                        <Form.Item {...formListSpan} name={[field.name, "unit"]} label="单位" rules={[
                                                            { required: true, message: '请输入数值单位' },
                                                            { max: 5, message: '数值单位长度不能超过5个字符' }
                                                        ]}>
                                                            <Input />
                                                        </Form.Item>
                                                    </Col>
                                                } else {

                                                    return (
                                                        <></>
                                                    );
                                                }
                                            }}

                                        </Form.Item>
                                        <Col className="content-center" span={2}>
                                            <Form.Item labelCol={{ span: 20 }} wrapperCol={{ span: 4 }} name={[field.name, "listAble"]} valuePropName="checked" label="列表字段">
                                                <Checkbox />
                                            </Form.Item>

                                        </Col>
                                        <Col className="content-center" span={3} >
                                            <Form.Item>

                                                <MinusCircleOutlined onClick={() => remove(index)} />
                                                &nbsp;&nbsp;
                                                <ArrowUpOutlined onClick={() => move(index, index - 1)} />
                                            </Form.Item>
                                        </Col>
                                    </Row>
                                );

                            })}
                            <Form.Item>
                                <Button type="dashed" onClick={() => add()} block >
                                    增加字段
                                </Button>
                            </Form.Item>
                        </>
                    )}
                </Form.List>

                <Form.Item >
                    <Space>

                        <Button type="primary" htmlType="submit">
                            提交
                        </Button>
                        <Button type="primary" onClick={()=>{
                            setPreviewFormVisitable(true)
                        }}>
                            预览表单
                        </Button>
                    </Space>
                </Form.Item>
            </Form>

            <Modal title='表单预览' open={previewFormVisitable} footer={null} onCancel={() => setPreviewFormVisitable(false)} width={800}>
                <FormNew workbenchId="" columnDefinitions={form.getFieldsValue().columns}></FormNew>
            </Modal>
        </div>
    );
};

export default BeanDesign;