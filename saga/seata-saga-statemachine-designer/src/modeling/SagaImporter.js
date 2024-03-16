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

function addArrayList(definitions) {
  const adjList = new Map();
  forEach(definitions.States, (semantic) => {
    // Initialize an array to store values for the key
    adjList.set(semantic, []);

    if (semantic.Next || semantic.CompensateState || semantic.Choices) {
      const options = [];

      if (semantic.Next) {
        options.push(semantic.Next);
      }

      if (semantic.CompensateState) {
        options.push(semantic.CompensateState);
      }

      if (semantic.Choices) {
        semantic.Choices.forEach((option) => options.push(option.Next));
      }

      const existingValues = adjList.get(semantic);
      options.forEach((next) => {
        existingValues.push(definitions.States[next]);
      });
      adjList.set(semantic, existingValues);
    }
  });
  return adjList;
}

function addCatchList(definitions, nodes) {
  const adjList = new Map();
  adjList.set(nodes, []);
  nodes.Catch.forEach((option) => {
    const { Next } = option;
    const existingValues = adjList.get(nodes);
    existingValues.push(definitions.States[Next]);
    adjList.set(nodes, existingValues);
  });
  return adjList;
}

export default function SagaImporter(
  sagaFactory,
  eventBus,
  canvas,
  elementFactory,
  elementRegistry,
  modeling,
) {
  this.sagaFactory = sagaFactory;
  this.eventBus = eventBus;
  this.canvas = canvas;
  this.elementRegistry = elementRegistry;
  this.elementFactory = elementFactory;
  this.modeling = modeling;
}

SagaImporter.$inject = [
  'sagaFactory',
  'eventBus',
  'canvas',
  'elementFactory',
  'elementRegistry',
  'modeling',
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
    let start = this.sagaFactory.create('StartState');
    let stateArrayList = new Map();
    start.importJson(definitions);
    let begin = start;
    start = this.add(start);
    const edges = [];
    const catches = [];
    forEach(definitions.States, (semantic) => {
      const state = this.sagaFactory.create(semantic.Type);

      if (semantic.style === undefined) {
        stateArrayList = addArrayList(definitions);
        state.importStates(definitions, semantic, begin, stateArrayList);
      } else {
        state.importJson(semantic);
        begin = state;
      }
      const host = this.add(state);

      if (semantic.edge === undefined) {
        state.importEdges(definitions, semantic);
        if (semantic.edge) {
          edges.push(...Object.values(semantic.edge));
        }
      } else {
        edges.push(...Object.values(semantic.edge));
      }

      if (semantic.Catch) {
        let source;
        if (semantic.catch === undefined) {
          const node = this.sagaFactory.create('Catch');
          const catchList = addCatchList(definitions, semantic);
          node.addCatch(definitions, semantic, catchList, stateArrayList);
          source = this.add(node);
        } else {
          const node = this.sagaFactory.create('Catch');
          node.importJson(semantic.catch);
          source = this.add(node);
        }

        if (semantic.catch.edge === undefined) {
          state.importCatchesEdges(definitions, semantic);
        }
        if (semantic.catch.edge) {
          semantic.Catch.forEach((exceptionMatch) => {
            if (semantic.catch.edge[exceptionMatch.Next]) {
              semantic.catch.edge[exceptionMatch.Next].Exceptions = exceptionMatch.Exceptions;
            }
          });

          this.modeling.updateAttachment(source, host);
          catches.push({
            source,
            edges: Object.values(semantic.catch.edge),
          });
        }
      }
    });

    if ((definitions.edge === undefined) && (definitions.States)) {
      start = this.sagaFactory.create('StartState');
      definitions.edge = {};
      start.importJsonEdges(definitions);
      if (definitions.edge) {
        const startEdge = this.sagaFactory.create('Transition');
        startEdge.importJson(definitions.edge);
        this.add(startEdge, { source: start });
      }
    }
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

    forEach(catches, (oneCatch) => {
      const {
        source,
        edges: exceptionMatches,
      } = oneCatch;
      forEach(exceptionMatches, (semantic) => {
        const exceptionMatch = this.sagaFactory.create(semantic.Type);
        exceptionMatch.importJson(semantic);
        this.add(exceptionMatch, { source });
      });
    });
  } catch (e) {
    error = e;
    console.error(error);
  }

  this.eventBus.fire('import.done', {
    error,
    warnings,
  });
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

    source = attrs.source || this.getSource(semantic);
    target = this.getTarget(semantic);
    semantic.style.source = source;
    semantic.style.target = target;

    if (source && target) {
      elementDefinition = elementData(semantic, {
        source,
        target,
        waypoints,
      });

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
