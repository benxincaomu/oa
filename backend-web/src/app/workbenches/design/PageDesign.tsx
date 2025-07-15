"use client"
import grapesjs, { Editor } from 'grapesjs';
import GjsEditor from '@grapesjs/react';
import webpage  from 'grapesjs-preset-webpage' ;
import gjsForms from 'grapesjs-plugin-forms';
import zh from "grapesjs/locale/zh";
import basic from 'grapesjs-blocks-basic';
import "grapesjs/dist/css/grapes.min.css";
type Props = {
    wid: number;
};
const PageDesign = ({ wid }: Props) => {

    const onEditor = (editor: Editor) => {
        console.log('Editor loaded', { editor });
    };

    return (
        <div className='he'>
            <GjsEditor
                // Pass the core GrapesJS library to the wrapper (required).
                // You can also pass the CDN url (eg. "https://unpkg.com/grapesjs")
                grapesjs={grapesjs}
                // Load the GrapesJS CSS file asynchronously from URL.
                // This is an optional prop, you can always import the CSS directly in your JS if you wish.
                /* grapesjsCss="https://unpkg.com/grapesjs/dist/css/grapes.min.css" */
                // GrapesJS init options
                options={{
                    height: '82vh',
                    storageManager: false,
                    i18n: {
                        messages:{zh}
                        
                    },
                    plugins: [
                        webpage,gjsForms,basic
                    ],
                    blockManager: {
                        
                    },


                }}
                onEditor={onEditor}




            />
        </div>
    );
};
export default PageDesign;