/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import React from '@bpmn-io/properties-panel/preact/compat';

export default function ExportControl(props) {
  const { editor } = props;

  function download(data, name, type) {
    const a = document.createElement('a');

    a.setAttribute(
      'href',
      `data:text/${type};charset=UTF-8,${encodeURIComponent(data)}`,
    );
    a.setAttribute('target', '_blank');
    a.setAttribute('dataTrack', `diagram:download-${type}`);
    a.setAttribute('download', `${name}.${type}`);

    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  return (
    <ul className="io-control io-control-list">
      <li>
        <button
          id="export-json"
          type="button"
          title="Export as state machine definition"
          onClick={() => {
            const raw = editor.export();
            download(JSON.stringify(raw), raw.Name, 'json');
          }}
        >
          <svg viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="1em">
            <path
              d="M512 823.838118l-292.321882-290.936471 42.465882-42.706823L481.882353 708.909176V58.548706h60.235294v650.36047l219.678118-218.654117 42.465882 42.706823L512 823.838118zM963.764706 543.924706v389.722353a30.117647 30.117647 0 0 1-30.117647 30.117647h-843.294118a30.117647 30.117647 0 0 1-30.117647-30.117647V543.623529H0V933.647059c0 49.814588 40.538353 90.352941 90.352941 90.352941h843.294118c49.814588 0 90.352941-40.538353 90.352941-90.352941V543.924706h-60.235294z"
            />
          </svg>
        </button>
      </li>
      <li className="vr" />
      <li>
        <button
          id="export-svg"
          type="button"
          title="Export as image"
          onClick={() => {
            const name = editor.get('canvas').getRootElement()?.businessObject?.Name || 'diagram';
            download(editor.exportSvg(), name, 'svg');
          }}
        >
          <svg viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="1em">
            <path
              d="M60.235294 60.235294v903.529412h903.529412V60.235294H60.235294z m843.294118 843.294118H120.470588v-120.470588h783.058824v120.470588zM120.470588 722.823529V120.470588h783.058824v602.352941H120.470588z m735.051294-110.531764l-41.984 43.188706-145.588706-141.492706-93.605647 100.954353-149.744941-265.938824-204.860235 299.670588-49.754353-33.972706 259.614118-379.663058 156.852706 278.407529 79.329882-85.654588 189.741176 184.500706z"
            />
          </svg>
        </button>
      </li>
    </ul>
  );
}
