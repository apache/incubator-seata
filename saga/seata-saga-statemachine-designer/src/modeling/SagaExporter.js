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
import StateMachine from '../spec/StateMachine';
import StartState from '../spec/StartState';
import State from '../spec/State';
import Edge from '../spec/style/Edge';

export default function SagaExporter(elementRegistry) {
  this.elementRegistry = elementRegistry;
}
SagaExporter.$inject = ['elementRegistry'];

SagaExporter.prototype.parseRoot = function (definitions, root) {
  const { businessObject } = root;
  assign(definitions, businessObject.exportJson());
};

SagaExporter.prototype.parseState = function (definitions, node) {
  const { businessObject } = node;
  const elementJson = businessObject.exportJson();

  const { Name } = businessObject;
  definitions.States[Name] = elementJson;
};

SagaExporter.prototype.parseEdge = function (definitions, edge) {
  const { businessObject } = edge;
  const elementJson = businessObject.exportJson();
  const { source, target } = elementJson.style;
  if (!source) {
    if (definitions.StartState) {
      throw new Error(`Two or more start states, ${target} and ${definitions.StartState}`);
    } else {
      definitions.StartState = target;
      if (definitions.edge === undefined) {
        definitions.edge = {};
      }
      assign(definitions.edge, elementJson);
    }
  } else {
    const stateRef = definitions.States[source];
    switch (businessObject.Type) {
      case 'ChoiceEntry':
        if (!stateRef.Choices) {
          stateRef.Choices = [];
        }
        stateRef.Choices.push({
          Expression: businessObject.Expression,
          Next: target,
        });
        if (businessObject.Default) {
          stateRef.Default = target;
        }
        stateRef.edge = assign(stateRef.edge || {}, { [target]: elementJson });
        break;
      case 'ExceptionMatch':
        stateRef.Catch.push({
          Exceptions: businessObject.Exceptions,
          Next: target,
        });
        stateRef.catch = assign(stateRef.catch || {}, { edge: { [target]: elementJson } });
        break;
      case 'Compensation':
        stateRef.CompensateState = target;
        stateRef.edge = assign(stateRef.edge || {}, { [target]: elementJson });
        break;
      case 'Transition':
      default:
        stateRef.Next = target;
        stateRef.edge = assign(stateRef.edge || {}, { [target]: elementJson });
    }
  }
};

SagaExporter.prototype.export = function () {
  const definitions = {};

  const elements = this.elementRegistry.getAll();
  const root = elements.filter(({ businessObject }) => businessObject instanceof StateMachine)[0];
  const start = elements.filter(({ businessObject }) => businessObject instanceof StartState)[0];
  const states = elements.filter(({ businessObject }) => businessObject instanceof State);
  const edges = elements.filter(({ businessObject }) => businessObject instanceof Edge);

  this.parseRoot(definitions, root);
  this.parseRoot(definitions, start);
  assign(definitions, { States: {} });
  states.forEach((state) => this.parseState(definitions, state));
  edges.forEach((edge) => this.parseEdge(definitions, edge));

  return definitions;
};
