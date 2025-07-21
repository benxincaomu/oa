import BpmnModeler from 'camunda-bpmn-js/lib/base/Modeler';
import PropertiesView from "./PropertiesView";
import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import { createRoot ,hydrateRoot} from 'react-dom/client';
interface PropertiesPanelProps {
    container: HTMLDivElement;
    bpmnModelerRef: React.RefObject<BpmnModeler | null>;
}
class PropertiesPanel {

    constructor({ container, bpmnModelerRef }: PropertiesPanelProps) {
        const root = createRoot(container);
        root.render(<PropertiesView bpmnModelerRef={bpmnModelerRef} />);
        
    }

    


};
export default PropertiesPanel;