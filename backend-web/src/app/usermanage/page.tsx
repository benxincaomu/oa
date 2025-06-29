"use client"; // 因为使用了 React State 和交互，必须是 Client Component

import { Button, Form, Input, message, Modal, Space, Table, Popconfirm } from "antd";
import { useState } from "react";
import axios from "axios";

interface User {

    id: number;
    name: string;
    email: string;
    userName: string;
    mobile: string;
}

const UserManager = () => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [addUserFrom] = Form.useForm();
    const [editUserForm] = Form.useForm();
    const [searchUserForm] = Form.useForm();

    const [users, setUsers] = useState<User[]>([]);
    const loadUsers = () => {
        // 模拟从服务器获取用户数据
        const userName = searchUserForm.getFieldValue("userNameS");
        const name = searchUserForm.getFieldValue("nameS");

        axios.get("/user/list", {
            params: {
                name: name,
                userName: userName,
            },
            headers: {
                token: localStorage.getItem("token") || "",
            },
        })
            .then((response) => {
                console.log("获取用户数据成功:", response.data);
                setUsers(response.data.data.content); // 假设后端返回 { users: [...] }
            })
            .catch((error) => {
                console.error("Error loading users:", error);
                message.error("加载用户失败");
            });
    };

    /**
     * 添加用户
     * @param values the form values
     */
    const handleAddUser = (values: any) => {
        /* const newUser: User = {
            
            id: users.length + 1,
            name: values.name,
            email: values.email,
        };
        setUsers([...users, newUser]);
        addUserFrom.resetFields();
        setIsModalOpen(false);
        message.success("用户添加成功"); */
    };

    const handleDelete = (id: number) => {
        axios.delete(`/user/${id}`, {
            headers: {
                token: localStorage.getItem("token") || "",
            },
        }).then(() => {
            message.success("用户删除成功");
            loadUsers();
        });
    };

    const columns = [
        {
            title: "ID",
            dataIndex: "id",
            key: "id",
        },
        {
            title: "用户名",
            dataIndex: "userName",
            key: "userName",
        }, {
            title: "姓名",
            dataIndex: "name",
            key: "name",
        },
        {
            title: "手机号",
            dataIndex: "mobile",
            key: "mobile",
        },
        {
            title: "邮箱",
            dataIndex: "email",
            key: "email",
        },
        {
            title: "操作",
            key: "action",
            render: (_: any, record: User) => (
                <Space size="middle">
                    <a onClick={() => console.log("编辑:", record)}>编辑</a>
                    <Popconfirm title="确定删除吗？" onConfirm={(e) => handleDelete(record.id)} okText="确定" cancelText="取消">

                        <a
                            onClick={() => {

                            }}
                            style={{ color: "red" }}
                        >
                            删除
                        </a>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    return (
        <div>
            <div
                style={{
                    marginBottom: 16,
                    display: "flex",
                    justifyContent: "space-between",
                }}
            >
                {/* 搜索框 */}
                <Form
                    form={searchUserForm}
                    layout="inline"
                    onFinish={() => {
                        loadUsers();
                    }}
                >
                    <Form.Item name="userNameS">
                        <Input placeholder="用户名" />
                    </Form.Item>
                    <Form.Item name="nameS">
                        <Input placeholder="姓名" />
                    </Form.Item>
                    <Form.Item>
                        <Button type="primary" htmlType="submit">查询</Button>
                    </Form.Item>
                </Form>
                <Button type="primary" onClick={() => setIsModalOpen(true)}>
                    新增用户
                </Button>
            </div>

            <Table
                dataSource={users}
                columns={columns}
                pagination={{ pageSize: 20 }}
                rowKey={(record) => record.id}
            />

            <Modal
                title="新增用户"
                open={isModalOpen}
                onCancel={() => setIsModalOpen(false)}
                footer={null}
            >
                <Form form={addUserFrom} onFinish={handleAddUser}>

                    <Input name="id" type="hidden" />

                    <Form.Item
                        label="用户名"
                        name="userName"
                        rules={[{ required: true }]}
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item
                        label="姓名"
                        name="name"
                        rules={[{ required: true }]}
                    >
                        <Input />
                    </Form.Item>
                    {/* <Form.Item
                        label="密码"
                        name="password"
                        rules={[{ required: true }]}
                    >
                        <Input.Password  />
                    </Form.Item>
                    <Form.Item
                        label="确认密码"
                        name="confirmPassword"
                        rules={[
                            { required: true, message: '请再次输入密码' },
                            {
                                validator(_, value) {
                                    if (!value || addUserFrom.getFieldValue('password') === value) {
                                        return Promise.resolve();
                                    }
                                    return Promise.reject(new Error('两次输入的密码不一致'));
                                },
                            },
                        ]}
                    >
                        <Input.Password />
                    </Form.Item> */}
                    <Form.Item
                        label="邮箱"
                        name="email"
                        rules={[]}
                    >
                        <Input />
                    </Form.Item>
                    <Form.Item
                        label="手机号码"
                        name="mobile"
                        rules={[]}
                    >
                        <Input />
                    </Form.Item>
                    <Button type="primary" htmlType="submit" block>
                        提交
                    </Button>
                </Form>
            </Modal>
        </div>
    );
};

export default UserManager;
