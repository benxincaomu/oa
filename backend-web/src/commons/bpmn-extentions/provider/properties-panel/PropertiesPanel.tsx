"use client"
import { is } from 'bpmn-js/lib/util/ModelUtil';
import BpmnModeler from 'camunda-bpmn-js/lib/base/Modeler';

import React, { Component, useState, useEffect } from 'react';
import ProcessPropertiesView from './ProcessPropertiesView';
import { Element } from 'bpmn-js/lib/model/Types';
import UserTaskPropertiesView from './UserTaskPropertiesView';
import StartEventPropertiesView from './StartEventPropertiesView';
import EndEventPropertiesView from './EndEventPropertiesView';
import SequenceFlowPropertiesView from './SequenceFlowView';
interface PropertiesPanelProps {
    bpmnModelerRef: React.RefObject<BpmnModeler | null>;
}
const PropertiesPanel = ({ bpmnModelerRef }: PropertiesPanelProps) => {
    const [selectedElements, setSelectedElements] = useState<Element[]>([]);

    const [bpmnType, setBpmnType] = useState<string>("");
    useEffect(() => {
        const modeler = bpmnModelerRef.current;
        if (modeler && selectedElements.length > 0) {
            setBpmnType(selectedElements[0].type);
            console.log(`type: ${selectedElements[0].type}`);
        } else {
            setBpmnType("");
        }

    }, [selectedElements, bpmnModelerRef]);


    useEffect(() => {
        const modeler = bpmnModelerRef.current;
        let mounted = true;

        if (modeler) {

            const selection = modeler.get('selection');
            // console.log('selection', selection);
            const handleSelectionChange = (event) => {
                if (mounted) {
                    setSelectedElements(event.newSelection);
                }
            };

            modeler.on('selection.changed', handleSelectionChange);

            // 返回清理函数
            return () => {
                modeler.off('selection.changed', handleSelectionChange);
                mounted = false;
            };
        }
    }, [bpmnModelerRef]);
    /**
     * type: bpmn:ParallelGateway
     * type: bpmn:ExclusiveGateway
         type: bpmn:EventBasedGateway
     */

    return (
        <div className="properties-panel">

            {
                (() => {
                    switch (bpmnType) {
                        case "bpmn:Process":
                            return <ProcessPropertiesView bpmnModelerRef={bpmnModelerRef} />
                        case "bpmn:UserTask":
                            return <UserTaskPropertiesView bpmnModelerRef={bpmnModelerRef} />
                        case "bpmn:StartEvent":
                            return <StartEventPropertiesView bpmnModelerRef={bpmnModelerRef} />
                        case "bpmn:EndEvent":
                            return <EndEventPropertiesView bpmnModelerRef={bpmnModelerRef} />
                        case "bpmn:SequenceFlow":
                            return <SequenceFlowPropertiesView bpmnModelerRef={bpmnModelerRef} bpmnId={selectedElements[0].id}/>
                        case "bpmn:ExclusiveGateway":
                            return <></>
                        case "bpmn:ExclusiveGateway":
                            return <></>
                        case "bpmn:EventBasedGateway":
                            return <></>
                        //
                        default:
                            return <ProcessPropertiesView bpmnModelerRef={bpmnModelerRef} />
                    }
                })()
            }
        </div>
    )
};



export default PropertiesPanel;

interface ElementPropertiesProps {
    element: any;
    bpmnModeler: BpmnModeler;
}
const ElementProperties = ({ element, bpmnModeler }: ElementPropertiesProps) => {
    const bpmnReplace = bpmnModeler.get('bpmnReplace');
};