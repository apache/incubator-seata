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

import {
  assign, forEach,
  map,
} from 'min-dash';

// helper /////
function elementData(semantic, attrs) {
  return assign({
    type: semantic.Type,
    businessObject: semantic,
  }, attrs);
}

function collectWaypoints(edge) {
  const { waypoints } = edge;

  if (waypoints) {
    return map(waypoints, (waypoint) => {
      const position = { x: waypoint.x, y: waypoint.y };

      return assign({ original: position }, position);
    });
  }
  return null;
}

export default function SagaImporter(
  sagaFactory,
  eventBus,
  canvas,
  elementFactory,
  elementRegistry,
) {
  this.sagaFactory = sagaFactory;
  this.eventBus = eventBus;
  this.canvas = canvas;
  this.elementRegistry = elementRegistry;
  this.elementFactory = elementFactory;
}

SagaImporter.$inject = [
  'sagaFactory',
  'eventBus',
  'canvas',
  'elementFactory',
  'elementRegistry',
];

SagaImporter.prototype.import = function (definitions) {
  let error = [];
  const warnings = [];

  this.eventBus.fire('import.start', { definitions });

  try {
    const root = this.sagaFactory.create('StateMachine');
    root.importJson(definitions);
    this.root(root);

    // Add start state
    const start = this.sagaFactory.create('StartState');
    start.importJson(definitions);
    this.add(start);

    const edges = [];
    forEach(definitions.States, (semantic) => {
      const state = this.sagaFactory.create(semantic.Type);
      state.importJson(semantic);
      this.add(state);
      if (semantic.edge) {
        edges.push(...Object.values(semantic.edge));
      }
    });

    // Add start edge
    if (definitions.edge) {
      const startEdge = this.sagaFactory.create('Transition');
      startEdge.importJson(definitions.edge);
      this.add(startEdge, { source: start });
    }

    forEach(edges, (semantic) => {
      const transition = this.sagaFactory.create(semantic.Type);
      transition.importJson(semantic);
      this.add(transition);
    });
  } catch (e) {
    error = e;
    console.error(error);
  }

  this.eventBus.fire('import.done', { error, warnings });
};

SagaImporter.prototype.root = function (semantic) {
  const element = this.elementFactory.createRoot(elementData(semantic));

  this.canvas.setRootElement(element);

  return element;
};

/**
 * Add drd element (semantic) to the canvas.
 */
SagaImporter.prototype.add = function (semantic, attrs = {}) {
  const { elementFactory } = this;
  const { canvas } = this;
  const { style } = semantic;

  let element; let waypoints; let source; let target; let elementDefinition; let
    bounds;

  if (style.Type === 'Node') {
    bounds = style.bounds;

    elementDefinition = elementData(semantic, {
      x: Math.round(bounds.x),
      y: Math.round(bounds.y),
      width: Math.round(bounds.width),
      height: Math.round(bounds.height),
    });
    element = elementFactory.createShape(elementDefinition);

    canvas.addShape(element);
  } else if (style.Type === 'Edge') {
    waypoints = collectWaypoints(style);

    source = this.getSource(semantic) || attrs.source;
    target = this.getTarget(semantic);
    semantic.style.source = source;
    semantic.style.target = target;

    if (source && target) {
      elementDefinition = elementData(semantic, {
        source,
        target,
        waypoints,
      });
      // console.log(elementDefinition);

      element = elementFactory.createConnection(elementDefinition);

      canvas.addConnection(element);
    }
  } else {
    throw new Error(`unknown di for element ${semantic.id}`);
  }

  return element;
};

SagaImporter.prototype.getSource = function (semantic) {
  return this.getShape(semantic.style.source);
};

SagaImporter.prototype.getTarget = function (semantic) {
  return this.getShape(semantic.style.target);
};

SagaImporter.prototype.getShape = function (name) {
  return this.elementRegistry.find((element) => element.businessObject.Name === name);
};
