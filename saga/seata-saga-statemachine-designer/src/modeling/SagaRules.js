/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import inherits from 'inherits-browser';

import RuleProvider from 'diagram-js/lib/features/rules/RuleProvider';

export default function SagaRules(injector) {
  injector.invoke(RuleProvider, this);
}

inherits(SagaRules, RuleProvider);

SagaRules.$inject = ['injector'];

function canConnect(source, target) {
  if (!source || !target) {
    return null;
  }

  if (target.parent !== source.parent || source === target) {
    return false;
  }
  return { type: 'Transition' };
}

SagaRules.prototype.canConnect = canConnect;

SagaRules.prototype.init = function () {
  this.addRule('shape.create', (context) => {
    const { target } = context;
    const { shape } = context;

    return target.parent === shape.target;
  });

  this.addRule('connection.create', (context) => {
    const { source } = context;
    const { target } = context;

    return canConnect(source, target);
  });

  this.addRule('connection.reconnect', (context) => {
    const { source } = context;
    const { target } = context;

    return canConnect(source, target);
  });
};
