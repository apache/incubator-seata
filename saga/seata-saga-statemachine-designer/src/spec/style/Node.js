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
import BaseSpec from '../BaseSpec';
import NodeStyle from './NodeStyle';
import { is } from '../../utils/index';
// import THUMBNAIL from '../icons/bpmn-icon-service-task.svg';

const OFFSET_X = 36; const OFFSET_Y = 18; const OFFSET_TARGET_X = 20;
const DEFAULT_X = 200; const DEFAULT_Y = 200; const OFFSET_TARGET_Y = 40;
const DEFAULT_WIDTH = 100; const DEFAULT_HEIGHT = 80;

export default class Node extends BaseSpec {
  style = new NodeStyle();

  importJson(json) {
    if (json.style === undefined) {
      json.style = {};
      json.style.bounds = {
        x: DEFAULT_X,
        y: DEFAULT_Y,
        width: OFFSET_X,
        height: OFFSET_X,
      };
    }
    assign(this.style.bounds, json.style.bounds);
  }

  isElementPresent(visited, target) {
    for (const element of visited) {
      if (element[0] === target[0] && element[1] === target[1]) {
        return false;
      }
    }
    return true;
  }

  addWaypoints(
    source,
    target,
    definitions,
    type,
    targetWidth,
    targetHeight,
    sourceWidth,
    sourceHeight,
  ) {
    const sourceX = definitions.States[source].style.bounds.x;
    const sourceY = definitions.States[source].style.bounds.y;
    const targetX = definitions.States[target].style.bounds.x;
    const targetY = definitions.States[target].style.bounds.y;
    let waypoints1 = [];
    if (type === 'Transition') {
      waypoints1 = [{
        x: sourceX + sourceWidth,
        y: sourceY + (sourceHeight / 2),
      }, {
        x: targetX - OFFSET_TARGET_X,
        y: targetY + (targetHeight / 2),
      }, {
        x: targetX,
        y: targetY + (targetHeight / 2),
      }];
    } else if (type === 'Compensation') {
      waypoints1 = [{
        x: sourceX + (sourceWidth / 2),
        y: sourceY + sourceHeight,
      }, {
        x: targetX + (targetWidth / 2),
        y: targetY - OFFSET_TARGET_X,
      }, {
        x: targetX + (targetWidth / 2),
        y: targetY,
      }];
    } else if (sourceX === targetX) {
      waypoints1 = [{
        x: sourceX + (sourceWidth / 2),
        y: sourceY + sourceHeight,
      }, {
        x: targetX + (targetWidth / 2),
        y: targetY - OFFSET_TARGET_X,
      }, {
        x: targetX + (targetWidth / 2),
        y: targetY,
      }];
    } else if (sourceY === targetY) {
      waypoints1 = [{
        x: sourceX + sourceWidth,
        y: sourceY + (sourceHeight / 2),
      }, {
        x: targetX - OFFSET_TARGET_X,
        y: targetY + (targetHeight / 2),
      }, {
        x: targetX,
        y: targetY + (targetHeight / 2),
      }];
    }

    return {
      style: {
        waypoints: waypoints1,
        source,
        target,
      },
      Type: type,
    };
  }

  importEdges(definitions, startState) {
    if (startState.Next) {
      this.addEdge(startState.Name, startState.Next, definitions, 'Transition', startState);
    }

    if (startState.CompensateState) {
      this.addEdge(startState.Name, startState.CompensateState, definitions, 'Compensation', startState);
    }

    if (startState.Choices) {
      for (const option of startState.Choices) {
        this.addEdge(startState.Name, option.Next, definitions, 'ChoiceEntry', startState);
      }
    }

    this.importJson(startState);
  }

  addEdge(source, target, definitions, type, startState) {
    const sourceWidth = this.calculateWidth(definitions.States[source]);
    const sourceHeight = this.calculateHeight(definitions.States[source]);
    const targetWidth = this.calculateWidth(definitions.States[target]);
    const targetHeight = this.calculateHeight(definitions.States[target]);

    const elementJson = this.addWaypoints(
      source,
      target,
      definitions,
      type,
      targetWidth,
      targetHeight,
      sourceWidth,
      sourceHeight,
    );
    startState.edge = Object.assign(startState.edge || {}, { [target]: elementJson });
  }

  calculateWidth(state) {
    if (is(state, 'Task')) {
      return 100;
    }
    if (is(state, 'Event')) {
      return 36;
    }
    if (is(state, 'Choice')) {
      return 50;
    }
    return 100;
  }

  calculateHeight(state) {
    if (is(state, 'Task')) {
      return 80;
    }
    if (is(state, 'Event')) {
      return 36;
    }
    if (is(state, 'Choice')) {
      return 50;
    }
    return 80;
  }

  importJsonEdges(json) {
    if (json.States) {
      const targetX = json.States[json.StartState].style.bounds.x;
      const targetY = json.States[json.StartState].style.bounds.y;
      json.edge = {
        style: {
          waypoints: [{
            x: DEFAULT_X + OFFSET_X,
            y: DEFAULT_X + OFFSET_Y,
          }, {
            x: targetX - OFFSET_TARGET_X,
            y: targetY + OFFSET_TARGET_Y,
          }, {
            x: targetX,
            y: targetY + OFFSET_TARGET_Y,
          }],
          target: json.StartState,
        },
        Type: 'Transition',
      };
    }
    this.importJson(json);
  }

  importCatchesEdges(definitions, startState) {
    const catchEdges = startState.Catch;
    for (const option of catchEdges) {
      const sourceX = definitions.States[startState.Name].catch.style.bounds.x;
      const sourceY = definitions.States[startState.Name].catch.style.bounds.y;
      const targetX = definitions.States[option.Next].style.bounds.x;
      const targetY = definitions.States[option.Next].style.bounds.y;
      let waypoints1 = [];
      if (is(option.Next, 'Task')) {
        waypoints1 = [{
          x: sourceX + OFFSET_Y,
          y: sourceY,
        }, {
          x: targetX + DEFAULT_WIDTH / 2,
          y: targetY + DEFAULT_WIDTH,
        }, {
          x: targetX + DEFAULT_WIDTH / 2,
          y: (targetY + DEFAULT_WIDTH) - OFFSET_TARGET_X,
        }];
      } else {
        waypoints1 = [{
          x: sourceX + OFFSET_Y,
          y: sourceY,
        }, {
          x: targetX + OFFSET_Y,
          y: (targetY + OFFSET_X) + OFFSET_TARGET_X,
        }, {
          x: targetX + OFFSET_Y,
          y: targetY + OFFSET_X,
        }];
      }
      startState.catch.edge = assign(startState.catch.edge || {}, {
        [option.Next]: {
          style: {
            waypoints: waypoints1,
            source: startState.Name,
            target: option.Next,
          },
          Type: 'ExceptionMatch',
        },
      });
    }
    this.importJson(startState.catch);
  }

  addCatch(definitions, node, catchList, adjList) {
    node.catch = {};
    if (node.Catch) {
      const {
        style: {
          bounds: {
            x,
            y,
          },
        },
      } = node;
      const newX = x;
      const newY = y;
      node.catch.style = {};
      node.catch.style.bounds = {
        x: newX + DEFAULT_WIDTH / 2,
        y: newY - OFFSET_TARGET_X,
        width: OFFSET_X,
        height: OFFSET_X,
      };
    }
    this.importJson(node.catch);

    let prev = node.catch;
    let width1;
    let height1;
    catchList.get(node).forEach((semantic) => {
      if (is(semantic, 'Task')) {
        width1 = DEFAULT_WIDTH;
        height1 = DEFAULT_HEIGHT;
      } else {
        width1 = OFFSET_X;
        height1 = OFFSET_X;
      }
      semantic.style = {};
      semantic.style.bounds = {
        x: prev.style.bounds.x - DEFAULT_WIDTH / 2,
        y: prev.style.bounds.y - DEFAULT_WIDTH,
        width: width1,
        height: height1,
      };
      prev = semantic;

      this.importStates(definitions, semantic, null, adjList);
    });
  }

  importStates(definitions, startState, begin, adjList) {
    const visited = [];
    const queue = [];
    if (begin !== null) {
      if (startState.style === undefined) {
        startState.style = {};
        if (startState.style.bounds === undefined) {
          startState.style.bounds = {
            x: begin.style.bounds.x + 150, // Adjust x-coordinate
            y: begin.style.bounds.y, // Adjust y-coordinate
            width: DEFAULT_WIDTH,
            height: DEFAULT_HEIGHT,
          };
        }
      }
      this.importJson(startState);
    }
    queue.push(startState);

    function setBounds(neighbor, x, y, width1, height1) {
      if (neighbor.style.bounds === undefined) {
        neighbor.style.bounds = {
          x, // Adjust x-coordinate
          y, // Adjust y-coordinate
          width: width1,
          height: height1,
        };
        visited.push([x, y]);
      }
    }

    while (queue.length) {
      const currentState = queue.shift();

      adjList.get(currentState).forEach((neighbor) => {
        if (neighbor.style === undefined) {
          neighbor.style = {};
          if (is(neighbor, 'End')) {
            const target = [];
            target.push(currentState.style.bounds.x + 150, currentState.style.bounds.y);
            if (this.isElementPresent(visited, target)) {
              setBounds(
                neighbor,
                currentState.style.bounds.x + 150,
                currentState.style.bounds.y,
                OFFSET_X,
                OFFSET_X,
              );
            } else {
              setBounds(
                neighbor,
                currentState.style.bounds.x,
                currentState.style.bounds.y + 150,
                OFFSET_X,
                OFFSET_X,
              );
            }
          }

          if (is(neighbor, 'Task') && !neighbor.IsForCompensation) {
            const target = [];
            target.push(currentState.style.bounds.x + 150, currentState.style.bounds.y);
            if (this.isElementPresent(visited, target)) {
              setBounds(
                neighbor,
                currentState.style.bounds.x + 150,
                currentState.style.bounds.y,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT,
              );
            } else {
              setBounds(
                neighbor,
                currentState.style.bounds.x,
                currentState.style.bounds.y + 150,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT,
              );
            }

            const { Name } = neighbor;
            queue.push(definitions.States[Name]);
          } else if (is(neighbor, 'Task') && neighbor.IsForCompensation) {
            const target = [];
            target.push(currentState.style.bounds.x, currentState.style.bounds.y + 150);
            if (this.isElementPresent(visited, target)) {
              setBounds(
                neighbor,
                currentState.style.bounds.x,
                currentState.style.bounds.y + 150,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT,
              );
            }
          } else if (is(neighbor, 'CompensationTrigger')) {
            setBounds(
              neighbor,
              currentState.style.bounds.x,
              currentState.style.bounds.y - 150,
              OFFSET_X,
              OFFSET_X,
            );
            const { Name } = neighbor;
            queue.push(definitions.States[Name]);
          } else if (is(neighbor, 'Choice')) {
            setBounds(
              neighbor,
              currentState.style.bounds.x + 150,
              currentState.style.bounds.y,
              50,
              50,
            );
            const { Name } = neighbor;
            queue.push(definitions.States[Name]);
          }
        }
      });
    }
  }

  exportJson() {
    return assign({}, { style: this.style });
  }
}
