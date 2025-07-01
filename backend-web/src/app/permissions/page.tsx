"use client"

import { Form, Input, Button, Select, Space, Table, Modal } from 'antd';
import { useState, useEffect } from 'react';
import axios from 'axios';
interface ParentPermission {
    id: number;
    name: string;

}

const Permissions = () => {
    const [searchForm] = Form.useForm();
    const columns = [
        {
            title: '权限名称',
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: '权限类型',
            dataIndex: 'type',
            key: 'type',
        },
        {
            title: '权限描述',
            dataIndex: 'description',
            key: 'description',
        },
        {
            title: "权限值",
            dataIndex: 'value',
            key: 'value',
        },
        {
            title: '操作',
            dataIndex: 'id',
            render: (_: any, record: any) => (
                <Space size="middle">
                    <a onClick={() => {
                        console.log("编辑:", record.id)
                    }}>编辑</a>
                </Space>
            )
        }
    ];
    const permissionData = [{
        id: 1,
        name: "用户管理",
        value: "/usermanage",
    }];

    // 添加权限相关
    const [addOpen, setAddOpen] = useState(false);
    const [addForm] = Form.useForm();
    const handleAddPermission = (values: any) => {
        setAddOpen(true);
        axios.post("/permission", values, {
            headers: {
                "Content-Type": "application/json",
                "token": localStorage.getItem("token"),
            },
        });
    };
    const [parentPermissions, setParentPermissions] = useState<ParentPermission[]>([]);
    const getParentPermissions = (type: number) => {
        let typeTmp = 0;
        switch (type) {
            case 1:
                typeTmp = 1;
                break;
            case 2:
                typeTmp = 1;
                break;
            case 3:
                typeTmp = 2;
                break;
            default:
                typeTmp = 0;
                break;
        }
        axios
            .get(`/permission/permissionsByType?type=${typeTmp}`, {
                headers: {
                    "token": localStorage.getItem("token"),
                },
            })
            .then((response) => {
                setParentPermissions(response.data.data);
            });
    };
    return (
        <div>
            <header>
                <title>权限管理</title>
            </header>
            <Form form={searchForm} layout="inline">
                <Form.Item label="权限名称" name="name">
                    <Input placeholder="请输入权限名称" />
                </Form.Item>
                <Form.Item label="权限类型" name="type">
                    <Select placeholder="请选择权限类型" defaultValue="">
                        <Select.Option value={null} >请选择</Select.Option>
                        <Select.Option value={1}>目录菜单</Select.Option>
                        <Select.Option value={2}>页面菜单</Select.Option>
                        <Select.Option value={3}>请求链接</Select.Option>
                        <Select.Option value={4}>页面控制</Select.Option>
                    </Select>
                </Form.Item>

                <Form.Item>

                    <Button type='primary'>查询</Button>
                </Form.Item>
                <Form.Item>

                    <Button type='primary' onClick={() => {
                        setAddOpen(true);
                    }}>添加权限</Button>
                </Form.Item>
            </Form>
            <Table columns={columns} dataSource={permissionData} rowKey={(record) => record.id} />
            <Modal title="添加权限" open={addOpen} onCancel={() => { setAddOpen(false) }} footer={null}>
                <Form form={addForm} onFinish={values => handleAddPermission(values)} layout='horizontal'>
                    <Form.Item label="权限名称" name="name">
                        <Input placeholder="请输入权限名称" />
                    </Form.Item>
                    <Form.Item label="权限描述" name="description">
                        <Input placeholder="请输入权限描述" />
                    </Form.Item>
                    <Form.Item label="权限类型" name="type">
                        <Select placeholder="请选择权限类型" onChange={(value) => getParentPermissions(value)}>
                            <Select.Option value={1}>目录菜单</Select.Option>
                            <Select.Option value={2}>页面菜单</Select.Option>
                            <Select.Option value={3}>请求链接</Select.Option>
                            <Select.Option value={4}>页面控制</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item
                        label="权限值"
                        name="value"
                        rules={[]}
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item label="上级权限" name="parentId" >
                        <Select placeholder="请选择上级权限" fieldNames={{ label: 'name', value: 'id' }} options={parentPermissions} />

                    </Form.Item>
                    <Form.Item labelCol={{ span: 4 }} wrapperCol={{ span: 8, offset: 16 }}>
                        <Button type='primary' htmlType='submit'>保存</Button>
                        <Button style={{ marginLeft: 8 }} onClick={() => setAddOpen(false)}>取消</Button>
                    </Form.Item>

                </Form>
            </Modal>
        </div>
    )
}
export default Permissions;