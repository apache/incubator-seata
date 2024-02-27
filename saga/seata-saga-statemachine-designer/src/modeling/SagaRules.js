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

import RuleProvider from 'diagram-js/lib/features/rules/RuleProvider';
import { getOrientation } from 'diagram-js/lib/layout/LayoutUtil';
import { is } from '../utils';

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

  if (is(source, 'Task') && is(target, 'Task') && target.businessObject.IsForCompensation) {
    return { type: 'Compensation' };
  }

  if (is(source, 'Choice')) {
    return { type: 'ChoiceEntry' };
  }

  if (is(source, 'Catch')) {
    return { type: 'ExceptionMatch' };
  }

  return { type: 'Transition' };
}

function canCreate(shapes, target) {
  let shapeList = shapes;
  if (!Array.isArray(shapes)) {
    shapeList = [shapes];
  }

  const invalid = shapeList.map((shape) => {
    if (is(shape, 'Catch')) {
      return false;
    }

    if (!target) {
      return true;
    }

    return target.parent === shape.target;
  }).filter((valid) => !valid).length;

  return !invalid;
}

function canAttach(shapes, target, position) {
  if (Array.isArray(shapes)) {
    if (shapes.length > 1) {
      return false;
    }
  }
  const shape = shapes[0] || shapes;

  if (is(shape, 'Catch')) {
    if (position && getOrientation(position, target, -15) === 'intersect') {
      return false;
    }

    if (is(target, 'Task')) {
      return 'attach';
    }
  }

  return false;
}

function canMove(shapes, target, position) {
  const shapeSet = new Set(shapes);
  // Exclude all catches with parents included
  const filtered = shapes.filter((shape) => !(is(shape, 'Catch') && shapeSet.has(shape.parent)));
  return !target || canAttach(filtered, target, position) || canCreate(filtered, target);
}

SagaRules.prototype.init = function () {
  this.addRule('shape.create', (context) => {
    const { target } = context;
    const { shape } = context;

    return canCreate(shape, target);
  });

  this.addRule('shape.attach', (context) => {
    const { shape, target, position } = context;

    return canAttach(shape, target, position);
  });

  this.addRule('elements.move', (context) => {
    const { shapes, target, position } = context;

    return canMove(shapes, target, position);
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
