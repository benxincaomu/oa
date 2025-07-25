import { Form, Input } from "antd";
import ModelerProps from "./ModelerProps";
import { useEffect } from "react";
import Modeling from "bpmn-js/lib/features/modeling/Modeling";
import { Moddle } from "bpmn-js/lib/features/modeling/ElementFactory";
import Selection from 'diagram-js/lib/features/selection/Selection';


export default function FlowConditionView({ bpmnModelerRef }: ModelerProps) {
    useEffect(() => {
        const bpmnModeler = bpmnModelerRef.current;
        if (bpmnModeler) {
            const moddle: Moddle = bpmnModeler.get("moddle");

            const modeling: Modeling = bpmnModeler.get("modeling");

        }
    }, [bpmnModelerRef]);
    const onConditionChange = (value: string) => {
        console.log('onConditionChange', value);
        const bpmnModeler = bpmnModelerRef.current;
        if(bpmnModeler && value){
            const moddle: Moddle = bpmnModeler.get("moddle");
            const selection:Selection = bpmnModeler.get('selection');
            const selectedElement = selection.get()[0];
            const modeling: Modeling = bpmnModeler.get("modeling");
            const conditionExpression = moddle.create('bpmn:FormalExpression', {
                body: value
            });
            modeling.updateProperties(selectedElement, {
                conditionExpression
            });
        }
    };

    return (
        <Form>
            <Form.Item label="spel条件" name="condition" required >
                <Input onChange={(e) => onConditionChange(e.target.value)} />
            </Form.Item>
        </Form>
    )
}