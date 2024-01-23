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

import { domify } from 'min-dom';
import React from '@bpmn-io/properties-panel/preact/compat';
import ImportControl from './ImportControl';
import ExportControl from './ExportControl';

export default function (editor) {
  const container = domify('<div class="io-control-list-container"/>');
  const canvas = editor.get('canvas');
  canvas._container.appendChild(container);

  React.render(
    <div style={{ position: 'fixed', bottom: '10px', left: '20px' }}>
      <ImportControl editor={editor} />
      <ExportControl editor={editor} />
    </div>,
    container,
  );
}
