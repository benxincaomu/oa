"use client"
import axios from 'axios';
import { message } from 'antd';
const service = axios.create({
    baseURL: '/', // 所有请求都会带上这个前缀
    timeout: 5000, // 超时时间
    headers: {
        'Content-Type': 'application/json',
    },
});

// 请求拦截器
service.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['token'] = token;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// 响应拦截器
service.interceptors.response.use(
    (response) => {
        const res = response.data;
        if (res.code === 10002) {
            localStorage.removeItem('token');
            window.location.reload();
        }else if (res.code !== 200) {
            message.error(res.message || '操作失败');
            return Promise.reject(new Error(res.message));
        }
        return res;
    },
    (error) => {
        message.error('网络请求失败');
        return Promise.reject(error);
    }
);


export default service;