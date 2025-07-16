import { Button, ButtonProps, Space ,Typography} from "antd";
import { useNode } from "@craftjs/core";
import React, { useState } from "react";
import ContentEditable from "react-contenteditable";

export const UserButton = ({ children, type = "primary" }: { children: React.ReactNode } & ButtonProps) => {
    const { connectors: { connect, drag } } = useNode();
    return (

        <Button type={type} ref={ref => connect(drag(ref))}>{children}</Button>
    )
        ;
};
/* UserButton.craft = {
    displayName: '用户按钮',
    props: {
        type: 'primary',
        children: '按钮'
    },
    related: {
        settings: () => null
    },

    rules: {
        canDrag: (node) => node.data.props.text != "Drag"
    }
} */

export const UserSpace = ({ children }: { children: React.ReactNode }) => {
    const { connectors: { connect, drag } } = useNode();
    return <span ref={ref => connect(drag(ref))}>
        <Space size="middle">{children}</Space>
    </span>;
};
UserSpace.craft = {
    displayName: '间隔',
    props: {
        type: 'primary',
        children: '间隔'
    },

    rules: {
        canDrag: (node) => node.data.props.text != "Drag"
    }
}
export const UserContainer = ({ children }: { children: React.ReactNode }) => {
    const { connectors: { connect, drag } } = useNode();
    return <div ref={ref => connect(drag(ref))} style={{ height: "100%", minHeight: "200px" }}>
        {children}
    </div>;
};
UserContainer.craft = {
    displayName: '用户容器',
    props: {
        type: 'primary',
        children: '容器'
    },
    related: {
        settings: () => null
    },
    rules: {
        canDrag: (node) => node.data.props.text != "Drag"
    }
}
const {Text} = Typography;
interface TextProps {
    text?: string;
    fontSize?: number;
}
export const EditorText = ({ text,fontSize}:TextProps) => {
    const { connectors: {connect, drag}, hasSelectedNode,actions: {setProp} } = useNode((state) => ({
    hasSelectedNode: state.events.selected,
    hasDraggedNode: state.events.dragged
}));;
const [disabled, setDisabled] = useState(true);
    return <div ref={ref => connect(drag(ref))}>
        <ContentEditable 
        html={text}
        onChange={(e) => {
            setProp(prop => props.text = e.target.value.replace(/<\/?[^>]+(>|$)/g, "" ));
        }}
        tagName="p"
        disabled={disabled}
        
        style={{fontSize: `${fontSize}px`}}
        />
    </div>;
};
