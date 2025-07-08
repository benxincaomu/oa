"use client"; // 因为使用了 React State 和交互，必须是 Client Component

import { Button, Form, Input, message, Modal, Space, Table, Popconfirm, Select } from "antd";
import { useEffect, useState } from "react";
import axios from "axios";
import service from "../base/service";

interface User {

    id: number;
    name: string;
    email: string;
    userName: string;
    mobile: string;
}
interface Role {
    id: number;
    name: string;
}

const UserManager = () => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [addUserFrom] = Form.useForm();
    const [editUserForm] = Form.useForm();
    const [searchUserForm] = Form.useForm();

    const [users, setUsers] = useState<User[]>([]);
    const [currPage, setCurrPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const loadUsers = () => {
        // 模拟从服务器获取用户数据
        const userName = searchUserForm.getFieldValue("userNameS");
        const name = searchUserForm.getFieldValue("nameS");

        axios.get("/user/list", {
            params: {
                name: name,
                userName: userName,
                currPage: currPage,
                pageSize: pageSize,
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
        axios.post("/user", {
            ...values
        }, {
            headers: {
                token: localStorage.getItem("token")
            }
        }).then((res) => {
            if (res.data.code === 200) {
                message.success("添加用户成功");
                addUserFrom.resetFields();
                setIsModalOpen(false);
            } else {
                message.error("添加用户失败");
            }
        });
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
                    {/* <a onClick={() => console.log("编辑:", record)}>编辑</a> */}
                    <Popconfirm title="确定删除吗？" onConfirm={(e) => handleDelete(record.id)} okText="确定" cancelText="取消">

                        <a
                            onClick={() => {

                            }}
                            style={{ color: "red" }}
                        >
                            删除
                        </a>
                    </Popconfirm>
                    <Popconfirm title="确定要禁用吗？" okText="确定" cancelText="取消">
                        <a
                            onClick={() => {

                            }}
                            style={{ color: "blue" }}
                        >
                            禁用
                        </a>
                    </Popconfirm>
                    <a>修改</a>
                    <a onClick={() => {
                        beforeAssignRole(record as User);
                    }}>分配角色</a>
                </Space>
            ),
        },
    ];

    // 角色相关
    const [assignRoleForm] = Form.useForm();
    const [roles, setRoles] = useState<Role[]>([]);
    useEffect(() => {
        setTimeout(() => {

            loadUsers();
        }, 1000);
    }, []);
    const beforeAssignRole = (user: User) => {
        if (roles.length == 0) {
            service.get("role/listAll").then((res) => {
                setRoles(res.data);
            });
        }
        service.get(`/user/getRoleIdByUserId/${user.id}`).then(res => {
            if (res.data) {
                setRoleId(res.data);
            }
            setOperatingUser(user as User);
            setUserId(user.id);
            setAssignOpen(true);
        });
    }
    const onAssignClose = () => {
        assignRoleForm.resetFields();
        setAssignOpen(false);
        setOperatingUser(nullUser);
        setUserId(0);
        setRoleId(0);
    };
    const nullUser = {
        id: 0,
        userName: "",
        name: "",
    } as User;
    const [operatingUser, setOperatingUser] = useState<User>(nullUser);
    const [userId, setUserId] = useState(0);
    const [assignOpen, setAssignOpen] = useState(false);
    //选定用户当前的id
    const [roleId, setRoleId] = useState(0);
    const onAssignFinish = (values: any) => {
        console.log("Received values of form: ", values);
        service.post("/user/assignRole", { ...values }).then(res => {
            message.success("分配成功");
            onAssignClose();
        });
    };
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
                    <Form.Item>

                        <Button type="primary" onClick={() => setIsModalOpen(true)}>
                            新增用户
                        </Button>
                    </Form.Item>
                </Form>
            </div>

            <Table
                dataSource={users}
                columns={columns}
                pagination={{ pageSize: 20 }}
                rowKey={(record) => record.id}
                onChange={(pagination, filters, sorter, extra) => {
                    setCurrPage(pagination.current || 1);
                    setPageSize(pagination.pageSize || 20);
                    loadUsers();
                }}
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
            <Modal
                title="分配角色"
                open={assignOpen}
                footer={null}
                onCancel={() => {
                    onAssignClose();
                }}
            >
                <Form
                    name="assign"
                    labelCol={{ span: 4 }}
                    wrapperCol={{ span: 20 }}
                    onFinish={(values) => {
                        onAssignFinish(values);
                    }}
                    autoComplete="off"
                    form={assignRoleForm}
                >
                    <Form.Item label="角色" name="roleId" rules={[{ required: true, message: '请选择角色' }]} >
                        <Select showSearch options={roles} fieldNames={{ label: 'name', value: 'id' }} defaultValue={roleId > 0 ? roleId : null} placeholder="请选择角色" />
                    </Form.Item>
                    <Form.Item name="userId" initialValue={userId}>
                        <Input type="hidden" />
                    </Form.Item>
                    <Form.Item labelCol={{ span: 4 }} wrapperCol={{ span: 8, offset: 16 }}>
                        <Button type="primary" htmlType="submit">
                            提交
                        </Button>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default UserManager;
