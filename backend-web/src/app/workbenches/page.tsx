"use client"
import { Button, Form, Input, message, Modal, Space, Table } from "antd";
import service from "../base/service";
import { useEffect, useState } from "react";
interface Workbench {
    id: number;
    name: string;
    description: string;
    createTime: string;
    updateTime: string;
}
const Workbench = () => {

    const [messageApi, contextHolder] = message.useMessage();
    const [seachForm] = Form.useForm();
    const [addForm] = Form.useForm();
    const loadWorkbench = (values: any) => {
        service.get(`/workbench/list`).then((res) => {
            setWorkbenches(res.data.content);
            messageApi.success("查询成功")
        })
    };
    useEffect(() => {
        document.title = "设计工作台";
        setTimeout(() => {
            loadWorkbench({});
        }, 300);
    }, []);

    const [addOpen, setAddOpen] = useState(false);

    const onAddWorkbench = (values: any) => {
        service.post("/workbench", {...values}).then((res) => {
            messageApi.info("添加成功");
            setAddOpen(false);
        });
    };
    const [workbenches, setWorkbenches] = useState<Workbench[]>([]);
    const columns = [
        {
            title: "名称",
            dataIndex: "name",
        },
        {
            title: "描述",
            dataIndex: "description",
        },
        {
            title: "操作",
            render: (text: any, record: any) => (
                <Space size="middle">
                    <a>编辑</a>
                    <a>删除</a>
                    <a target="_blank" href={`/workbenches/design?wid=${record.id}`}>设计</a>
                </Space>
            )
        }
    ]

    return (
        <div>
            {contextHolder}
            <div className="text-2xl font-bold">设计工作台</div>
            <Form form={seachForm} onFinish={(values) => {loadWorkbench(values)}} layout="inline"> 
                <Form.Item label="名称" name="name"><Input placeholder="请输入名称" /></Form.Item>
                <Form.Item>
                    <Button type="primary" htmlType="submit">查询</Button>
                </Form.Item>
                <Form.Item>
                    <Button type="primary" onClick={() => { 
                        setAddOpen(true);
                    }}>新增</Button>

                </Form.Item>
            </Form>
            <Modal title="新增设计稿" open={addOpen} onCancel={() => { setAddOpen(false) }} footer={null} > 
                <Form form={addForm} layout="horizontal" onFinish={(values) => {onAddWorkbench(values)}}>
                    <Form.Item label="名称" name={"name"}>
                        <Input  />
                    </Form.Item>
                    <Form.Item label="描述" name={"description"}>
                        <Input />
                    </Form.Item>
                    <Form.Item > 
                        <Button type="primary" htmlType="submit">
                            提交
                        </Button>
                        &nbsp;&nbsp;
                        <Button onClick={() => {
                            
                        }}>
                            取消
                        </Button>
                    </Form.Item>
                </Form>
            </Modal>
            <Table columns={columns} dataSource={workbenches} rowKey={"id"}/>
        </div>
    );
};

export default Workbench;