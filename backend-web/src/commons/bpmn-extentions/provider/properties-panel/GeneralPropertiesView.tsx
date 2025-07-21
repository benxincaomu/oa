import BpmnModeler from 'camunda-bpmn-js/lib/base/Modeler';

import React, { Component, useState, useEffect } from 'react';
import type { CollapseProps } from 'antd';
import { Collapse, Form, Input } from 'antd';

import ModelerProps from './ModelerProps';
/**
 * 
 * 通用属性面板
 * @returns 
 */
export default function GeneralPropertiesView({ bpmnModelerRef }: ModelerProps) {
    const [form] = Form.useForm();

    useEffect(() => {
        const modeler = bpmnModelerRef.current;
        if (modeler) {
            const modeling = modeler.get("modeling");
            const selection = modeler.get('selection');
            // console.log('selection', selection.get());
            if (selection.get().length > 0) {
                const element = selection.get()[0];
                console.log('element', element);
                form.setFieldValue("id", element.id);
                form.setFieldValue("name", element.businessObject.name);
            } else {
                const canvas = modeler.get("canvas");
                form.setFieldValue("id", canvas.getRootElement().id);
                form.setFieldValue("name", canvas.getRootElement().businessObject.name);
            }
        }
    }, [bpmnModelerRef]);

    const onIdChange = (e: any) => {
        const modeler = bpmnModelerRef.current;
        if (modeler) {
            const modeling = modeler.get("modeling");
            const selection = modeler.get('selection');
            const id = form.getFieldValue("id");
            let element = null;
            if (selection.get().length > 0) {
                element = selection.get()[0];
                
            } else {
                const canvas = modeler.get("canvas");
                element = canvas.getRootElement();
            }
            modeling.updateProperties(element, {
                id: id
            });
        }
    }

    const onNameChange = (e: any) => {
        const modeler = bpmnModelerRef.current;
        if (modeler) {
            const modeling = modeler.get("modeling");
            const selection = modeler.get('selection');
            let name = form.getFieldValue("name");
            let element = null;
            if (selection.get().length > 0) {
                element = selection.get()[0];
                
            } else {
                const canvas = modeler.get("canvas");
                element = canvas.getRootElement();
            }
            modeling.updateProperties(element, {
                name: name
            });
        }
    }

    return (
        <Form form={form} colon={false} layout="vertical">
            <Form.Item label="名称" name={"name"} >
                <Input onChange={(e) => onNameChange(e)} />
            </Form.Item>
            <Form.Item label="ID" name={"id"} >
                <Input onChange={(e) => onIdChange(e)} />
            </Form.Item>
            {/* <Form.Item label="version tag" name={"versionTag"}>
                <Input />
            </Form.Item> */}
        </Form>
    )
}