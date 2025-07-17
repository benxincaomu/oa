"use client"

import { useEffect, useState,useRef } from 'react';
import { usePathname, useSearchParams } from 'next/navigation';
import service from '@/app/base/service';
import { Tabs } from 'antd';
import BeanDesign from './BeanDesign';
import PageDesign from './PageDesign';
import WorkflowDesign from './WorkflowDesign';
import { PageEditor } from '@/page_editor/editor';
import BpmnModeler from 'camunda-bpmn-js/lib/camunda-platform/Modeler';
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
  const bpmnModelerRef = useRef<BpmnModeler | null>(null);
  const tabData = [
    {
      key: '0',
      label: '枚举设计',
      children: <div>枚举设计</div>,
    },
    {
      key: '1',
      label: '实体设计',
      children: <BeanDesign wid={wid} />,
    },
    {
      key: '2',
      label: '流程设计',
      children: <WorkflowDesign wid={wid} bpmnModelerRef={bpmnModelerRef} />,
    },
   /*  {
      key: '3',
      label: '页面设计',
      children: <PageDesign wid={wid} />,
    },
    {
      key: '4',
      label: '编辑器',
      children: <PageEditor/>,
    } */
  ]
  return (


    <Tabs defaultActiveKey="2" items={tabData} />


  );
};
export default Design;