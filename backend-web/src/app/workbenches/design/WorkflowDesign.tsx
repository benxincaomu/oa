"use client"
import React, { useRef, useEffect } from 'react';
// import BpmnModeler from 'bpmn-js/lib/Modeler';
import BpmnModeler from 'camunda-bpmn-js/lib/camunda-platform/Modeler';

import 'camunda-bpmn-js/dist/assets/camunda-platform-modeler.css';

// 引入 BPMN.js 和属性面板所需的基本样式
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';

import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';
import '@bpmn-io/properties-panel/assets/properties-panel.css';

import { BpmnPropertiesPanelModule, BpmnPropertiesProviderModule } from 'bpmn-js-properties-panel';

// 如果你需要 Camunda 特有属性，保留这两个导入：
import CamundaModdleExtension from 'camunda-bpmn-moddle/resources/camunda.json';
import { Button, Space } from 'antd';
import zh from 'bpmn-js-i18n/translations/zn';


// 定义组件的 Props 接口 (现在可以更简单)
interface BpmnModelerComponentProps {
    wid: number,
    onSave?: (xml: string) => void;
    onPublish?: (xml: string) => void;
    bpmnModelerRef: React.RefObject<BpmnModeler | null>;

}
function customTranslate(template, replacements) {
  replacements = replacements || {};

  // Translate
  template = zh[template] || template;

  // Replace
  return template.replace(/{([^}]+)}/g, function(_, key) {
    return replacements[key] || '{' + key + '}';
  });
}

const WorkflowDesign: React.FC<BpmnModelerComponentProps> = ({
    wid, bpmnModelerRef
}) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const propertiesPanelRef = useRef<HTMLDivElement>(null);


    const customTranslateModule = {
        translate: ['value', customTranslate] // 'zh' is the imported translation dictionary
    };

    useEffect(() => {
        if (!containerRef.current || !propertiesPanelRef.current) {
            console.error('BPMN container or properties panel ref is null.');
            return;
        }

        // 确保只初始化一次 Modeler
        if (!bpmnModelerRef.current) {
            const modeler = new BpmnModeler({
                container: containerRef.current,
                height: '78vh',
                /* width: '70%', */
                propertiesPanel: {
                    parent: propertiesPanelRef.current,
                },
                additionalModules: [
                    // BpmnPropertiesPanelModule,
                    // BpmnPropertiesProviderModule,
                    customTranslateModule,
                    //   CamundaExtensionModule, // 如果不需要 Camunda 属性，可以移除此行
                ],
                moddleExtensions: {
                    camunda: CamundaModdleExtension, // 如果不需要 Camunda 属性，可以移除此行
                },
                camunda: CamundaModdleExtension,
                /* keyboard: {
                    bindTo: document,
                },  */
            });
            modeler.createDiagram();
            bpmnModelerRef.current = modeler;


        }

        // 清理函数：在组件卸载时销毁 Modeler 实例
        return () => {
            if (bpmnModelerRef.current) {
                bpmnModelerRef.current.destroy();
                bpmnModelerRef.current = null;
            }
        };
    }, []); // 确保此 useEffect 只在组件挂载时运行一次

    const onSaveXML = () => {
        bpmnModelerRef.current?.saveXML().then(({ xml }) => {
            console.log(xml);
        });
    };

    return (

        <div className='height-100'>
            <Space align='end'>
                <Button type='primary' onClick={() => {
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