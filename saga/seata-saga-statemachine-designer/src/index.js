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

import PropertiesPanel from './properties-panel';
import PropertiesProvider from './properties-panel/provider';
import Editor from './Editor';
import control from './control';
import { randomString } from './utils';

const editor = new Editor({
  container: document.querySelector('#canvas'),
  keyboard: { bindTo: document },
  propertiesPanel: { parent: '#properties' },
  // Add properties panel as additional modules
  additionalModules: [
    PropertiesPanel,
    PropertiesProvider,
  ],
});

control(editor);

editor.import({
  Name: `StateMachine-${randomString()}`,
  Comment: 'This state machine is modeled by designer tools.',
  Version: '0.0.1',
  style: {
    bounds: {
      x: 200,
      y: 200,
      width: 36,
      height: 36,
    },
  },
});
