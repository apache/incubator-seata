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

import { Group } from '@bpmn-io/properties-panel';

import NameProps from './properties/NameProps';
import CommentProps from './properties/CommentProps';
import VersionProps from './properties/VersionProps';
import StateProps from './properties/StateProps';
import StyleProps from './properties/StyleProps';
import { is } from '../../utils';

function GeneralGroup(element) {
  const entries = [
    ...NameProps({ element }),
    ...CommentProps({ element }),
  ];

  if (is(element, 'StateMachine')) {
    entries.push(...VersionProps({ element }));
  }

  if (is(element, 'Connection') || is(element, 'StartState') || is(element, 'Catch')) {
    return null;
  }

  return {
    id: 'general',
    label: 'General',
    entries,
    component: Group,
  };
}

function JsonGroup(element) {
  const entries = [
    ...StateProps({ element }),
    ...StyleProps({ element }),
  ];

  if (is(element, 'Transition') || is(element, 'Compensation') || is(element, 'StartState') || is(element, 'Catch')) {
    entries.splice(0, 1);
  }

  return {
    id: 'json',
    label: 'Json Props',
    entries,
    shouldOpen: true,
    component: Group,
  };
}

function getGroups(element) {
  const groups = [
    GeneralGroup(element),
    JsonGroup(element),
  ];

  // contract: if a group returns null, it should not be displayed at all
  return groups.filter((group) => group !== null);
}

export default class PropertiesProvider {
  constructor(propertiesPanel) {
    propertiesPanel.registerProvider(this);
  }

  getGroups(element) {
    return (groups) => {
      return [
        ...groups,
        ...getGroups(element),
      ];
    };
  }
}

PropertiesProvider.$inject = ['propertiesPanel'];
