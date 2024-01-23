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

import inherits from 'inherits-browser';

import { keys } from 'min-dash';
import BaseModeling from 'diagram-js/lib/features/modeling/Modeling';
import { getProperties, setProperties } from '../utils';

function UpdatePropertiesHandler() {
}

/**
 * @param {Object} context
 * @param {djs.model.Base} context.element the element to update
 * @param {Object} context.properties a list of properties to set on the element's businessObject.
 *
 * @return {Array<djs.model.Base>} the updated element
 */
UpdatePropertiesHandler.prototype.execute = function (context) {
  const { element } = context;
  const changed = [element];

  if (!element) {
    throw new Error('element required');
  }

  const { businessObject } = element;
  const { properties } = context;
  const oldProperties = context.oldProperties || getProperties(businessObject, keys(properties));

  const { override } = context;
  // update properties
  setProperties(businessObject, properties, override);

  // store old values
  context.oldProperties = oldProperties;
  context.changed = changed;

  // indicate changed on objects affected by the update
  return changed;
};

/**
 * @param  {Object} context
 *
 * @return {djs.model.Base} the updated element
 */
UpdatePropertiesHandler.prototype.revert = function (context) {
  const { element } = context;
  const { oldProperties } = context;
  const { businessObject } = element;

  // update properties
  setProperties(businessObject, oldProperties);
  return context.changed;
};

export default function Modeling(
  canvas,
  commandStack,
  rules,
  injector,
) {
  this.canvas = canvas;
  this.commandStack = commandStack;
  this.rules = rules;

  injector.invoke(BaseModeling, this);
}

inherits(Modeling, BaseModeling);

Modeling.$inject = [
  'canvas',
  'commandStack',
  'rules',
  'injector',
];

Modeling.prototype.connect = function (source, target, attrs, hints) {
  const { rules } = this;
  const rootElement = this.canvas.getRootElement();

  if (!attrs) {
    attrs = rules.canConnect(source, target);
  }

  return this.createConnection(source, target, attrs, rootElement, hints);
};

Modeling.prototype.getHandlers = function () {
  const handlers = BaseModeling.prototype.getHandlers.call(this);

  handlers['element.updateProperties'] = UpdatePropertiesHandler;

  return handlers;
};

Modeling.prototype.updateProperties = function (element, properties, override) {
  this.commandStack.execute('element.updateProperties', {
    element,
    properties,
    override,
  });
};
