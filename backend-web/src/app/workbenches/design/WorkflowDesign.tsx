"use client"
import React, { useRef, useEffect } from 'react';
import BpmnModeler from 'bpmn-js/lib/Modeler';
// import CamundaExtensionModule from 'camunda-bpmn-moddle';

// 引入 BPMN.js 和属性面板所需的基本样式
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';

import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';
import '@bpmn-io/properties-panel/assets/properties-panel.css';

import { BpmnPropertiesPanelModule, BpmnPropertiesProviderModule } from 'bpmn-js-properties-panel';

// 如果你需要 Camunda 特有属性，保留这两个导入：
import CamundaModdleExtension from 'camunda-bpmn-moddle/resources/camunda.json';
import { Button, Space } from 'antd';


// 定义组件的 Props 接口 (现在可以更简单)
interface BpmnModelerComponentProps {
    wid:number
}

const WorkflowDesign: React.FC<BpmnModelerComponentProps> = ({
    wid
}) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const propertiesPanelRef = useRef<HTMLDivElement>(null);
    const bpmnModelerRef = useRef<BpmnModeler | null>(null);


    useEffect(() => {
        if (!containerRef.current || !propertiesPanelRef.current) {
            console.error('BPMN container or properties panel ref is null.');
            return;
        }

        // 确保只初始化一次 Modeler
        if (!bpmnModelerRef.current) {
            const modeler = new BpmnModeler({
                container: containerRef.current,
                height: '81vh',
                /* width: '70%', */
                propertiesPanel: {
                    parent: propertiesPanelRef.current,
                },
                additionalModules: [
                    BpmnPropertiesPanelModule,
                    BpmnPropertiesProviderModule,
                    //   CamundaExtensionModule, // 如果不需要 Camunda 属性，可以移除此行
                ],
                moddleExtensions: {
                    camunda: CamundaModdleExtension, // 如果不需要 Camunda 属性，可以移除此行
                },
                /* keyboard: {
                    bindTo: document,
                },  */
            });
            modeler.createDiagram();
            bpmnModelerRef.current = modeler;
            const bpmnXML = `
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn2:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xsi:schemaLocation="http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd" id="sample-diagram" targetNamespace="http://bpmn.io/schema/bpmn">
                <bpmn2:process id="Process_1" isExecutable="false">
                    <bpmn2:startEvent id="StartEvent_1"/>
                </bpmn2:process>
                <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
                    <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
                        <dc:Bounds height="36.0" width="36.0" x="412.0" y="240.0"/>
                    </bpmndi:BPMNShape>
                    </bpmndi:BPMNPlane>
                </bpmndi:BPMNDiagram>
                </bpmn2:definitions>
            `;

            /* modeler.importXML(bpmnXML).then(() => {
                console.log("BPMN diagram loaded successfully");
                // modeler.get('canvas').zoom('fit');
            }).catch((err) => {
                console.error("Failed to import BPMN XML", err);
            }); */

        }

        // 清理函数：在组件卸载时销毁 Modeler 实例
        return () => {
            if (bpmnModelerRef.current) {
                bpmnModelerRef.current.destroy();
                bpmnModelerRef.current = null;
            }
        };
    }, []); // 确保此 useEffect 只在组件挂载时运行一次

    const onSaveXML =  () => { 
        bpmnModelerRef.current?.saveXML().then(({ xml }) => {
            console.log(xml);
        });
    };

    return (

        <div className='height-100'>
            <Space align='end'>
                <Button type='primary' onClick={()=>{
                    onSaveXML();
                }}>保存</Button>
                <Button type='primary' danger>清空</Button>
                </Space>
            <div style={{ display: 'flex', height: '100%' }}>

                <div ref={containerRef} style={{
                    flexGrow: 1, // 占据剩余空间
                    border: '1px solid #ccc',
                    minHeight: '300px',
                }} />

                <div
                    ref={propertiesPanelRef}
                    style={{
                        width: '280px', // 固定宽度
                        borderLeft: '1px solid #ccc',
                        overflowY: 'auto',
                        padding: '10px',
                    }} />
            </div>
        </div>
    );
};
export default WorkflowDesign;