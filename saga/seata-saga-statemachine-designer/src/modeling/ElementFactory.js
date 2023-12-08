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

import { assign } from 'min-dash';

import inherits from 'inherits-browser';

import BaseElementFactory from 'diagram-js/lib/core/ElementFactory';

/**
 * A drd-aware factory for diagram-js shapes
 */
export default function ElementFactory(sagaFactory) {
  BaseElementFactory.call(this);

  this.sagaFactory = sagaFactory;
}

inherits(ElementFactory, BaseElementFactory);

ElementFactory.$inject = ['sagaFactory'];

ElementFactory.prototype.baseCreate = BaseElementFactory.prototype.create;

ElementFactory.prototype.create = function (elementType, attrs) {
  const { sagaFactory } = this;

  attrs = attrs || {};

  let { businessObject } = attrs;

  if (!businessObject) {
    if (!attrs.type) {
      throw new Error('no shape type specified');
    }

    businessObject = sagaFactory.create(attrs.type);
  }

  const size = sagaFactory.getDefaultSize(businessObject);

  attrs = assign({ businessObject }, size, attrs);

  return this.baseCreate(elementType, attrs);
};
