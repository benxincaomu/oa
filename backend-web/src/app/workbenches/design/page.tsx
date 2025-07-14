"use client"

import { useEffect, useState } from 'react';
import { usePathname, useSearchParams } from 'next/navigation';
import service from '@/app/base/service';
import { Tabs } from 'antd';
import BeanDesign from './BeanDesign';

const Design = () => {
  const params = useSearchParams();
  const [wid, setWid] = useState<number>(0);
  useEffect(() => {
    if (params) {
      
      const wid = params.get('wid');
      getWorkbench(Number(wid));
      setWid(Number(wid));
    }
  }, []);
  const getWorkbench = (wid: number) => {
    service.get(`/workbench/${wid}`).then((res) => {
      // console.log(res.data);
      document.title = `流程设计:${res.data.name}`;
    })
  };
  const tabData = [
    {
      key: '1',
      label: '实体设计',
      children: <BeanDesign wid={wid}/>,
    },
    {
      key: '2',
      label: '流程设计',
      children: <div>流程设计</div>,
    },
    {
      key: '3',
      label: '页面设计',
      children: <div>页面设计</div>,
    }
  ]
  return (
    <div >
      {/* <h1 className="text-3xl font-bold underline">Design</h1> */}
      <Tabs defaultActiveKey="1" items={tabData} />
    </div>
  );
};
export default Design;