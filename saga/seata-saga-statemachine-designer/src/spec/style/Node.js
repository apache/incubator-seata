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
// import THUMBNAIL from '../icons/bpmn-icon-service-task.svg';

export default class Node extends BaseSpec {
  style = new NodeStyle();

  importJson(json) {
    if (json.style === undefined) {
      json.style = {};
      json.style.bounds = {
        x: 200,
        y: 200,
        width: 36,
        height: 36,
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
        x: targetX - 20,
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
        y: targetY - 20,
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
        y: targetY - 20,
      }, {
        x: targetX + (targetWidth / 2),
        y: targetY,
      }];
    } else if (sourceY === targetY) {
      waypoints1 = [{
        x: sourceX + sourceWidth,
        y: sourceY + (sourceHeight / 2),
      }, {
        x: targetX - 20,
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
    return (state.Type === 'ServiceTask' || state.Type === 'ScriptTask' || state.Type === 'SubStateMachine') ? 100 : 36;
  }

  calculateHeight(state) {
    return (state.Type === 'ServiceTask' || state.Type === 'ScriptTask' || state.Type === 'SubStateMachine') ? 80 : 36;
  }

  importJsonEdges(json) {
    if (json.States) {
      const targetX = json.States[json.StartState].style.bounds.x;
      const targetY = json.States[json.StartState].style.bounds.y;
      json.edge = {
        style: {
          waypoints: [{
            x: 200 + 36,
            y: 200 + 18,
          }, {
            x: targetX - 20,
            y: targetY + 40,
          }, {
            x: targetX,
            y: targetY + 40,
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
      if ((definitions.States[option.Next].Type === 'ServiceTask')
        || (definitions.States[option.Next].Type === 'ScriptTask')
        || (definitions.States[option.Next].Type === 'SubStateMachine')) {
        waypoints1 = [{
          x: sourceX + 18,
          y: sourceY,
        }, {
          x: targetX + 50,
          y: targetY + 100,
        }, {
          x: targetX + 50,
          y: (targetY + 100) - 20,
        }];
      } else {
        waypoints1 = [{
          x: sourceX + 18,
          y: sourceY,
        }, {
          x: targetX + 18,
          y: (targetY + 36) + 20,
        }, {
          x: targetX + 18,
          y: targetY + 36,
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
        x: newX + 50,
        y: newY - 20,
        width: 36,
        height: 36,
      };
    }
    this.importJson(node.catch);

    let prev = node.catch;
    let width1;
    let height1;
    catchList.get(node).forEach((semantic) => {
      if ((semantic.Type === 'ServiceTask') || (semantic.Type === 'ScriptTask') || (semantic.Type === 'SubStateMachine')) {
        width1 = 100;
        height1 = 80;
      } else {
        width1 = 36;
        height1 = 36;
      }
      semantic.style = {};
      semantic.style.bounds = {
        x: prev.style.bounds.x - 50,
        y: prev.style.bounds.y - 100,
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
            width: 100,
            height: 80,
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
          if ((neighbor.Type === 'Fail') || (neighbor.Type === 'Succeed')) {
            const target = [];
            target.push(currentState.style.bounds.x + 150, currentState.style.bounds.y);
            if (this.isElementPresent(visited, target)) {
              setBounds(
                neighbor,
                currentState.style.bounds.x + 150,
                currentState.style.bounds.y,
                36,
                36,
              );
            } else {
              setBounds(
                neighbor,
                currentState.style.bounds.x,
                currentState.style.bounds.y + 150,
                36,
                36,
              );
            }
          }

          if ((neighbor.Type === 'ServiceTask' && !neighbor.IsForCompensation) || (neighbor.Type === 'ScriptTask' && !neighbor.IsForCompensation) || (neighbor.Type === 'SubStateMachine' && !neighbor.IsForCompensation)) {
            const target = [];
            target.push(currentState.style.bounds.x + 150, currentState.style.bounds.y);
            if (this.isElementPresent(visited, target)) {
              setBounds(
                neighbor,
                currentState.style.bounds.x + 150,
                currentState.style.bounds.y,
                100,
                80,
              );
            } else {
              setBounds(
                neighbor,
                currentState.style.bounds.x,
                currentState.style.bounds.y + 150,
                100,
                80,
              );
            }

            const { Name } = neighbor;
            queue.push(definitions.States[Name]);
          } else if ((neighbor.Type === 'ServiceTask' && neighbor.IsForCompensation) || (neighbor.Type === 'ScriptTask' && neighbor.IsForCompensation) || ((neighbor.Type === 'SubStateMachine' && neighbor.IsForCompensation))) {
            const target = [];
            target.push(currentState.style.bounds.x, currentState.style.bounds.y + 150);
            if (this.isElementPresent(visited, target)) {
              setBounds(
                neighbor,
                currentState.style.bounds.x,
                currentState.style.bounds.y + 150,
                100,
                80,
              );
            }
          } else if (neighbor.Type === 'CompensationTrigger') {
            setBounds(
              neighbor,
              currentState.style.bounds.x,
              currentState.style.bounds.y - 150,
              36,
              36,
            );
            const { Name } = neighbor;
            queue.push(definitions.States[Name]);
          } else if (neighbor.Type === 'Choice') {
            setBounds(
              neighbor,
              currentState.style.bounds.x + 150,
              currentState.style.bounds.y,
              36,
              36,
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
