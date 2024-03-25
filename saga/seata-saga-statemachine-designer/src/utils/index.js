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

import { forEach, reduce } from 'min-dash';
import { useContext } from '@bpmn-io/properties-panel/preact/hooks';
import PropertiesPanelContext from '../properties-panel/PropertiesPanelContext';

/**
 * Returns a random generated string for initial decision definition id.
 * @returns {string}
 */
export function randomString() {
  // noinspection SpellCheckingInspection
  const chars = 'abcdefghijklmnopqrstuvwxyz1234567890';
  const maxPos = chars.length;
  let str = '';
  for (let i = 0; i < 7; i++) {
    str += chars.charAt(Math.floor(Math.random() * maxPos));
  }
  return str;
}

export function useService(type, strict) {
  const { getService } = useContext(PropertiesPanelContext);

  return getService(type, strict);
}

export function getProperties(businessObject, propertyNames) {
  return reduce(propertyNames, (result, key) => {
    result[key] = businessObject[key];
    return result;
  }, {});
}

export function setProperties(businessObject, properties, override) {
  if (override) {
    Object.keys(businessObject)
      .filter((key) => key !== 'style')
      .forEach((key) => delete businessObject[key]);
  }
  forEach(properties, (value, key) => {
    businessObject[key] = value;
  });
}

export function is(element, target) {
  const type = element?.businessObject?.Type || element?.Type || element;

  if (target === 'Event') {
    return type === 'StartState' || type === 'CompensationTrigger' || type === 'Catch' || type === 'Fail' || type === 'Succeed';
  }

  if (target === 'End') {
    return type === 'Fail' || type === 'Succeed';
  }

  if (target === 'Task') {
    return type === 'ServiceTask' || type === 'ScriptTask' || type === 'SubStateMachine';
  }

  if (target === 'Connection') {
    return type === 'Transition' || type === 'ChoiceEntry' || type === 'ExceptionMatch' || type === 'Compensation';
  }

  return type === target;
}
