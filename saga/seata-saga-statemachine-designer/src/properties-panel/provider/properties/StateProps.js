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

import { assign } from 'min-dash';
import { useService } from '../../../utils';

function State(props) {
  const { element } = props;

  const debounce = useService('debounceInput');
  const modeling = useService('modeling');

  const options = {
    component: TextAreaEntry,
    element,
    id: 'props',
    // label: 'Props',
    debounce,
    autoResize: true,
    getValue: (e) => {
      const value = assign({}, e.businessObject);
      // Exclude style
      delete value.style;
      // Exclude Catch for Task
      delete value.Catch;
      return JSON.stringify(value, null, 2);
    },
    validate: (value) => {
      try {
        JSON.parse(value);
      } catch (e) {
        return e.message;
      }

      return null;
    },
    setValue: (value, newValidationError) => {
      try {
        JSON.parse(value);
      } catch (e) {
        newValidationError = e;
      }
      if (newValidationError) {
        return;
      }
      const businessObject = JSON.parse(value);
      modeling.updateProperties(element, businessObject, true);
    },
  };

  return CollapsibleEntry({
    id: 'collapsible-props',
    label: 'Props',
    element,
    entries: [options],
    open: true,
  });
}

export default function StateProps(props) {
  const {
    element,
  } = props;

  return [
    {
      component: State,
      element,
      isEdited: isTextFieldEntryEdited,
    },
  ];
}
