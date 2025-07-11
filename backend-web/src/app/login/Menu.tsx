"use client"
import { useState, useEffect } from "react";
import service from "../base/service";
import { Menu } from 'antd';
import { usePathname, useSearchParams } from 'next/navigation';

interface MenuItem {
    id: number;
    name: string;
    key: string;
    label: string;
    value: string;
    children?: MenuItem[];
}
const fillMenuItems = (data: MenuItem[]) => {
    data.forEach((item: any) => {
        item.key = item.id + "";
        item.label = item.name;
        item.type = item.type;
        delete item.parentId;
        if (item.children && item.children.length > 0) {
            fillMenuItems(item.children);
        }
    });
}
const findSelectedKeys = (data: MenuItem[], value?: string) => {
    if (value == null) {
        return [];
    }
    for (var i = 0; i < data.length; i++) {
        const item = data[i];
        if (item.value === value) {
            console.log("item.value:", item.value);
            return [item.key + ""];
        } else if (item.children && item.children.length > 0) {
            return findSelectedKeys(item.children, value);
        }
    }

}
// 获取祖先 key 路径
const getAncestorKeys = (data: MenuItem[], selectedKey: string): string[] => {
    const path: string[] = [];

    const findPath = (items: MenuItem[], targetKey: string): boolean => {
        for (const item of items) {
            if (item.key === targetKey) {
                return true;
            }

            if (item.children) {
                const found = findPath(item.children, targetKey);
                if (found) {
                    path.push(item.key);
                    return true;
                }
            }
        }
        return false;
    };

    findPath(data, selectedKey);
    return path;
};


const SideMenu = () => {
    const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
    const pathName = usePathname();
    const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
    const [openKeys, setOpenKeys] = useState<string[]>([]);
    // console.log("pathName", pathName);
    useEffect(() => {
        setTimeout(() => {
            service.get("/user/getMyMenus").then((res) => {
                // console.log(res.data);
                fillMenuItems(res.data as MenuItem[])
                setMenuItems(res.data as MenuItem[]);
                const keys = findSelectedKeys(res.data as MenuItem[], pathName as string);
                if(typeof keys !== "undefined"){
                    setSelectedKeys(keys);
                    if (keys.length > 0) {
                        const ancestorKeys = getAncestorKeys(res.data as MenuItem[], keys[0]);
                        setOpenKeys(ancestorKeys);
                    }
                }
            });
        }, 300);
    }, []);
    /* useEffect(() => {
        console.log("menuItems:", menuItems);
    }, [menuItems]); */

    const onClickMenu = ({ item, key, keyPath, domEvent }) => {
        console.log("item:", item);
        if (item.props.value) {
            window.location.href = item.props.value;
        }
    };

    return (<>
        <Menu
            mode="inline"
            theme="dark"
            items={menuItems}
            selectable
            onClick={({ item, key, keyPath, domEvent }) => {
                onClickMenu({ item, key, keyPath, domEvent });
            }}
            selectedKeys={selectedKeys}
            openKeys={openKeys}
            onOpenChange={(keys) => setOpenKeys(keys)}
        />
    </>);
};
export default SideMenu;