"use client"
import axios from 'axios';
import { message } from 'antd';
import Cookies from 'universal-cookie';
const service = axios.create({
    timeout: 5000, // 超时时间
    headers: {
        'Content-Type': 'application/json',
    },
});

// 请求拦截器
service.interceptors.request.use(
    (config) => {
        const cookies = new Cookies();
        const token = cookies.get('token');
        console.log('token', token);
        if (!token){
            message.error('请先登录');
            return new Promise(() => {});
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
            const cookies = new Cookies();
            cookies.remove('token',{path:'/'});
            // window.location.reload();
        }else if (res.code !== 200) {
            message.error(res.message || '操作失败');
            return new Promise(() => {});
        }
        return res;
    },
    (error) => {
        message.error('网络请求失败',error);
        return new Promise(() => {});
    }
);


export default service;