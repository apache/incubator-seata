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

import {
  TextAreaEntry,
  isTextFieldEntryEdited, CollapsibleEntry,
} from '@bpmn-io/properties-panel';

import { useService } from '../../../utils';

function Style(props) {
  const { element } = props;

  const debounce = useService('debounceInput');

  const options = {
    component: TextAreaEntry,
    element,
    id: 'style',
    debounce,
    autoResize: true,
    disabled: true,
    getValue: (e) => {
      return JSON.stringify(e.businessObject.style, null, 2);
    },
  };

  return CollapsibleEntry({
    id: 'collapsible-props',
    label: 'Style',
    element,
    entries: [options],
  });
}

export default function StateProps(props) {
  const {
    element,
  } = props;

  return [
    {
      component: Style,
      element,
      isEdited: isTextFieldEntryEdited,
    },
  ];
}
