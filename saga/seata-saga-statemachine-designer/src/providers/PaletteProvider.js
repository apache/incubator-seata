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

import { assign } from 'min-dash';
import ServiceTask from '../spec/ServiceTask';
import StartState from '../spec/StartState';
import ScriptTask from '../spec/ScriptTask';
import Choice from '../spec/Choice';
import Succeed from '../spec/Succeed';
import Fail from '../spec/Fail';
import Catch from '../spec/Catch';
import CompensationTrigger from '../spec/CompensationTrigger';
import SubStateMachine from '../spec/SubStateMachine';

const SPEC_LIST = [StartState, ServiceTask, ScriptTask, SubStateMachine, Choice, Succeed, Fail,
  Catch, CompensationTrigger];

/**
 * A palette provider.
 */
export default function PaletteProvider(create, elementFactory, lassoTool, palette) {
  this.create = create;
  this.elementFactory = elementFactory;
  this.lassoTool = lassoTool;
  this.palette = palette;

  palette.registerProvider(this);
}

PaletteProvider.$inject = [
  'create',
  'elementFactory',
  'lassoTool',
  'palette',
];

PaletteProvider.prototype.getPaletteEntries = function () {
  const { create } = this;
  const { elementFactory } = this;
  const { lassoTool } = this;

  function createAction(type, group, className, title, options) {
    function createListener(event) {
      const shape = elementFactory.createShape(assign({ type }, options));
      create.start(event, shape);
    }

    return {
      group,
      className,
      title,
      action: {
        dragstart: createListener,
        click: createListener,
      },
    };
  }

  const entries = {
    'lasso-tool': {
      group: 'tools',
      className: 'palette-icon-lasso-tool',
      title: 'Activate Lasso Tool',
      action: {
        click(event) {
          lassoTool.activateSelection(event);
        },
      },
    },
    'tool-separator': {
      group: 'tools',
      separator: true,
    },
  };
  SPEC_LIST.forEach((Spec) => {
    const type = Spec.prototype.Type;
    entries[`create-${type}`] = createAction(type, 'state', Spec.prototype.THUMBNAIL_CLASS, `Create ${type}`);
  });
  return entries;
};
