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

import { assign, forEach, isObject } from 'min-dash';

import { attr as domAttr, query as domQuery } from 'min-dom';

import { append as svgAppend, attr as svgAttr, create as svgCreate } from 'tiny-svg';

import BaseRenderer from 'diagram-js/lib/draw/BaseRenderer';

import { createLine } from 'diagram-js/lib/util/RenderUtil';
import { translate } from 'diagram-js/lib/util/SvgTransformUtil';

const BLACK = 'hsl(225, 10%, 15%)';
const TASK_BORDER_RADIUS = 10;
const DEFAULT_FILL_OPACITY = 0.95;

// helper functions //////////////////////

function getSemantic(element) {
  return element.businessObject;
}

function colorEscape(str) {
  // only allow characters and numbers
  return str.replace(/[^0-9a-zA-z]+/g, '_');
}

function getStrokeColor(element, defaultColor) {
  return defaultColor;
}

function getFillColor(element, defaultColor) {
  return defaultColor;
}

function getLabelColor(element, defaultColor, defaultStrokeColor) {
  return defaultColor || getStrokeColor(element, defaultStrokeColor);
}

export default function Renderer(config, eventBus, pathMap, styles, textRenderer, canvas) {
  BaseRenderer.call(this, eventBus);

  const { computeStyle } = styles;

  const markers = {};

  const defaultFillColor = (config && config.defaultFillColor) || 'white';
  const defaultStrokeColor = (config && config.defaultStrokeColor) || BLACK;
  const defaultLabelColor = (config && config.defaultLabelColor);

  function shapeStyle(attrs) {
    return styles.computeStyle(attrs, {
      strokeLinecap: 'round',
      strokeLinejoin: 'round',
      stroke: BLACK,
      strokeWidth: 2,
      fill: 'white',
    });
  }

  function addMarker(id, options) {
    const attrs = assign({
      strokeWidth: 1,
      strokeLinecap: 'round',
      strokeDasharray: 'none',
    }, options.attrs);

    const ref = options.ref || { x: 0, y: 0 };

    const scale = options.scale || 1;

    // fix for safari / chrome / firefox bug not correctly
    // resetting stroke dash array
    if (attrs.strokeDasharray === 'none') {
      attrs.strokeDasharray = [10000, 1];
    }

    const markerElement = svgCreate('marker');

    svgAttr(options.element, attrs);

    svgAppend(markerElement, options.element);

    svgAttr(markerElement, {
      id,
      viewBox: '0 0 20 20',
      refX: ref.x,
      refY: ref.y,
      markerWidth: 20 * scale,
      markerHeight: 20 * scale,
      orient: 'auto',
    });

    // eslint-disable-next-line no-underscore-dangle
    let defs = domQuery('defs', canvas._svg);

    if (!defs) {
      defs = svgCreate('defs');

      // eslint-disable-next-line no-underscore-dangle
      svgAppend(canvas._svg, defs);
    }

    svgAppend(defs, markerElement);

    markers[id] = markerElement;
  }

  function createMarker(id, type, fill, stroke) {
    const end = svgCreate('path');
    svgAttr(end, { d: 'M 1 5 L 11 10 L 1 15 Z' });

    if (type === 'connection-end') {
      addMarker(id, {
        element: end,
        attrs: {
          fill: stroke,
          stroke: 'none',
        },
        ref: { x: 11, y: 10 },
        scale: 1,
      });
    }

    if (type === 'default-choice-marker') {
      const defaultChoiceMarker = svgCreate('path', {
        d: 'M 6 4 L 10 16',
        ...shapeStyle({
          stroke,
        }),
      });

      addMarker(id, {
        element: defaultChoiceMarker,
        ref: { x: 0, y: 10 },
        scale: 1,
      });
    }
  }

  function marker(type, fill, stroke) {
    const id = `${type}-${colorEscape(fill)
    }-${colorEscape(stroke)}`;

    if (!markers[id]) {
      createMarker(id, type, fill, stroke);
    }

    return `url(#${id})`;
  }

  function drawCircle(parentGfx, width, height, offset, attrs) {
    if (isObject(offset)) {
      attrs = offset;
      offset = 0;
    }

    offset = offset || 0;

    attrs = shapeStyle(attrs);

    if (attrs.fill === 'none') {
      delete attrs.fillOpacity;
    }

    const cx = width / 2;
    const cy = height / 2;

    const circle = svgCreate('circle', {
      cx,
      cy,
      r: Math.round((width + height) / 4 - offset),
      ...attrs,
    });

    svgAppend(parentGfx, circle);

    return circle;
  }

  function drawRect(p, width, height, r, offset, attrs) {
    if (isObject(offset)) {
      attrs = offset;
      offset = 0;
    }

    offset = offset || 0;

    attrs = computeStyle(attrs, {
      stroke: BLACK,
      strokeWidth: 2,
      fill: 'white',
    });

    const rect = svgCreate('rect');
    svgAttr(rect, {
      x: offset,
      y: offset,
      width: width - offset * 2,
      height: height - offset * 2,
      rx: r,
      ry: r,
    });
    svgAttr(rect, attrs);

    svgAppend(p, rect);

    return rect;
  }

  function renderLabel(p, label, options) {
    const text = textRenderer.createText(label || '', options);

    domAttr(text, 'class', 'djs-label');

    svgAppend(p, text);

    return text;
  }

  function renderEmbeddedLabel(p, element, align, options) {
    const { Name } = element.businessObject;

    options = assign({
      box: element,
      align,
      padding: 5,
      style: {
        fill: getLabelColor(element, defaultLabelColor, defaultStrokeColor),
      },
    }, options);

    return renderLabel(p, Name, options);
  }

  function drawPath(p, d, attrs) {
    attrs = computeStyle(attrs, ['no-fill'], {
      strokeWidth: 2,
      stroke: BLACK,
    });

    const path = svgCreate('path');
    svgAttr(path, { d });
    svgAttr(path, attrs);

    svgAppend(p, path);

    return path;
  }

  function drawDiamond(parentGfx, width, height, attrs) {
    const x2 = width / 2;
    const y2 = height / 2;

    const points = [
      { x: x2, y: 0 },
      { x: width, y: y2 },
      { x: x2, y: height },
      { x: 0, y: y2 },
    ];

    const pointsString = points.map((point) => {
      return `${point.x},${point.y}`;
    }).join(' ');

    attrs = shapeStyle(attrs);

    const polygon = svgCreate('polygon', {
      ...attrs,
      points: pointsString,
    });

    svgAppend(parentGfx, polygon);

    return polygon;
  }

  function drawLine(p, waypoints, attrs) {
    attrs = computeStyle(attrs, ['no-fill'], {
      stroke: BLACK,
      strokeWidth: 2,
      fill: 'none',
    });

    const line = createLine(waypoints, attrs);

    svgAppend(p, line);

    return line;
  }

  function drawMarker(type, parentGfx, path, attrs) {
    return drawPath(parentGfx, path, assign({ 'data-marker': type }, attrs));
  }

  let handlers;

  function renderer(type) {
    return handlers[type];
  }

  function attachTaskMarkers(p, element, taskMarkers) {
    const obj = getSemantic(element);

    const sub = taskMarkers && taskMarkers.indexOf('SubStateMachineMarker') !== -1;
    let position;

    if (sub) {
      position = {
        seq: -21,
        parallel: -22,
        compensation: -42,
        loop: -18,
      };
    } else {
      position = {
        seq: -3,
        parallel: -6,
        compensation: -27,
        loop: 0,
      };
    }

    forEach(taskMarkers, (m) => {
      renderer(m)(p, element, position);
    });

    if (obj.IsForCompensation) {
      renderer('CompensationMarker')(p, element, position);
    }

    const { Loop } = obj;

    if (Loop) {
      renderer('LoopMarker')(p, element, position);
    }
  }

  handlers = {
    Transition(p, element) {
      const fill = getFillColor(element, defaultFillColor);
      const stroke = getStrokeColor(element, defaultStrokeColor);
      const attrs = {
        stroke,
        strokeWidth: 1,
        strokeLinecap: 'round',
        strokeLinejoin: 'round',
        markerEnd: marker('connection-end', fill, stroke),
      };

      return drawLine(p, element.waypoints, attrs);
    },
    ChoiceEntry(p, element) {
      const fill = getFillColor(element, defaultFillColor);
      const stroke = getStrokeColor(element, defaultStrokeColor);
      const attrs = {
        stroke,
        strokeWidth: 1,
        strokeLinecap: 'round',
        strokeLinejoin: 'round',
        markerEnd: marker('connection-end', fill, stroke),
      };

      const path = drawLine(p, element.waypoints, attrs);

      if (getSemantic(element).Default) {
        svgAttr(path, {
          markerStart: marker('default-choice-marker', fill, stroke),
        });
      }

      return path;
    },
    ExceptionMatch(p, element) {
      return renderer('Transition')(p, element);
    },
    Compensation(p, element) {
      const stroke = getStrokeColor(element, defaultStrokeColor);
      const attrs = {
        stroke,
        strokeWidth: 1,
        strokeLinecap: 'round',
        strokeLinejoin: 'round',
        strokeDasharray: '10, 11',
      };

      return drawLine(p, element.waypoints, attrs);
    },
    StartState(parentGfx, element) {
      return drawCircle(parentGfx, element.width, element.height, {
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      });
    },
    Task(parentGfx, element, additionalMarkers) {
      const attrs = {
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
        fillOpacity: DEFAULT_FILL_OPACITY,
      };

      const rect = drawRect(parentGfx, element.width, element.height, TASK_BORDER_RADIUS, attrs);

      renderEmbeddedLabel(parentGfx, element, 'center-middle');
      attachTaskMarkers(parentGfx, element, additionalMarkers);

      return rect;
    },
    ServiceTask(parentGfx, element) {
      const task = renderer('Task')(parentGfx, element);
      const pathDataBG = pathMap.getScaledPath('TASK_TYPE_SERVICE', {
        abspos: {
          x: 12,
          y: 18,
        },
      });

      /* service bg */ drawPath(parentGfx, pathDataBG, {
        strokeWidth: 1,
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      });

      const fillPathData = pathMap.getScaledPath('TASK_TYPE_SERVICE_FILL', {
        abspos: {
          x: 17.2,
          y: 18,
        },
      });

      /* service fill */ drawPath(parentGfx, fillPathData, {
        strokeWidth: 0,
        fill: getFillColor(element, defaultFillColor),
      });

      const pathData = pathMap.getScaledPath('TASK_TYPE_SERVICE', {
        abspos: {
          x: 17,
          y: 22,
        },
      });

      /* service */ drawPath(parentGfx, pathData, {
        strokeWidth: 1,
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      });

      return task;
    },
    ScriptTask(parentGfx, element) {
      const task = renderer('Task')(parentGfx, element);
      const pathData = pathMap.getScaledPath('TASK_TYPE_SCRIPT', {
        abspos: {
          x: 15,
          y: 20,
        },
      });

      /* script path */ drawPath(parentGfx, pathData, {
        strokeWidth: 1,
        stroke: getStrokeColor(element, defaultStrokeColor),
      });

      return task;
    },
    SubStateMachine(parentGfx, element) {
      return renderer('Task')(parentGfx, element, ['SubStateMachineMarker']);
    },
    SubStateMachineMarker(parentGfx, element) {
      const markerRect = drawRect(parentGfx, 14, 14, 0, {
        strokeWidth: 1,
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      });

      translate(markerRect, element.width / 2 - 7.5, element.height - 20);

      const markerPath = pathMap.getScaledPath('MARKER_SUB_PROCESS', {
        xScaleFactor: 1.5,
        yScaleFactor: 1.5,
        containerWidth: element.width,
        containerHeight: element.height,
        position: {
          mx: (element.width / 2 - 7.5) / element.width,
          my: (element.height - 20) / element.height,
        },
      });

      drawMarker('sub-process', parentGfx, markerPath, {
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      });
    },
    LoopMarker(parentGfx, element, position) {
      const markerPath = pathMap.getScaledPath('MARKER_LOOP', {
        xScaleFactor: 1,
        yScaleFactor: 1,
        containerWidth: element.width,
        containerHeight: element.height,
        position: {
          mx: ((element.width / 2 + position.loop) / element.width),
          my: (element.height - 7) / element.height,
        },
      });

      drawMarker('loop', parentGfx, markerPath, {
        strokeWidth: 1.5,
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
        strokeMiterlimit: 0.5,
      });
    },
    CompensationMarker(parentGfx, element, position) {
      const markerMath = pathMap.getScaledPath('MARKER_COMPENSATION', {
        xScaleFactor: 1,
        yScaleFactor: 1,
        containerWidth: element.width,
        containerHeight: element.height,
        position: {
          mx: ((element.width / 2 + position.compensation) / element.width),
          my: (element.height - 13) / element.height,
        },
      });

      drawMarker('compensation', parentGfx, markerMath, {
        strokeWidth: 1,
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      });
    },
    Gateway(parentGfx, element) {
      return drawDiamond(parentGfx, element.width, element.height, {
        fill: getFillColor(element, defaultFillColor),
        fillOpacity: DEFAULT_FILL_OPACITY,
        stroke: getStrokeColor(element, defaultStrokeColor),
      });
    },
    Choice(parentGfx, element) {
      const diamond = renderer('Gateway')(parentGfx, element);

      const pathData = pathMap.getScaledPath('GATEWAY_EXCLUSIVE', {
        xScaleFactor: 0.4,
        yScaleFactor: 0.4,
        containerWidth: element.width,
        containerHeight: element.height,
        position: {
          mx: 0.32,
          my: 0.3,
        },
      });

      drawPath(parentGfx, pathData, {
        strokeWidth: 1,
        fill: getStrokeColor(element, defaultStrokeColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      });

      return diamond;
    },
    Succeed(parentGfx, element) {
      return drawCircle(parentGfx, element.width, element.height, {
        strokeWidth: 4,
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      });
    },
    Error(parentGfx, element, fill) {
      const pathData = pathMap.getScaledPath('EVENT_ERROR', {
        xScaleFactor: 1.1,
        yScaleFactor: 1.1,
        containerWidth: element.width,
        containerHeight: element.height,
        position: {
          mx: 0.2,
          my: 0.722,
        },
      });
      return drawPath(parentGfx, pathData, {
        strokeWidth: 1,
        fill: fill ? getStrokeColor(element, defaultStrokeColor) : 'none',
        stroke: getStrokeColor(element, defaultStrokeColor),
      });
    },
    Fail(parentGfx, element) {
      const circle = handlers.Succeed(parentGfx, element);
      renderer('Error')(parentGfx, element, true);
      return circle;
    },
    Event(parentGfx, element) {
      const attrs = {
        strokeWidth: 1.5,
        fill: getFillColor(element, defaultFillColor),
        stroke: getStrokeColor(element, defaultStrokeColor),
      };

      // apply fillOpacity
      const outerAttrs = {
        ...attrs,
        fillOpacity: 1,
      };

      // apply no-fill
      const innerAttrs = {
        ...attrs,
        fill: 'none',
      };

      const outer = drawCircle(parentGfx, element.width, element.height, outerAttrs);
      drawCircle(parentGfx, element.width, element.height, 3, innerAttrs);
      return outer;
    },
    Catch(parentGfx, element) {
      const outer = renderer('Event')(parentGfx, element);
      renderer('Error')(parentGfx, element);

      return outer;
    },
    CompensationTrigger(parentGfx, element) {
      const outer = renderer('Event')(parentGfx, element);
      const pathData = pathMap.getScaledPath('EVENT_COMPENSATION', {
        xScaleFactor: 1,
        yScaleFactor: 1,
        containerWidth: element.width,
        containerHeight: element.height,
        position: {
          mx: 0.22,
          my: 0.5,
        },
      });

      const fill = 'none';

      drawPath(parentGfx, pathData, {
        strokeWidth: 1,
        fill,
        stroke: getStrokeColor(element, defaultStrokeColor),
      });
      return outer;
    },
  };
  function drawShape(parent, element) {
    const h = handlers[element.type];

    if (!h) {
      return BaseRenderer.prototype.drawShape.apply(this, [parent, element]);
    }
    return h(parent, element);
  }

  function drawConnection(parent, element) {
    const { type } = element;
    const h = handlers[type];

    if (!h) {
      return BaseRenderer.prototype.drawConnection.apply(this, [parent, element]);
    }
    return h(parent, element);
  }

  // eslint-disable-next-line no-unused-vars
  this.canRender = function (element) {
    return true;
  };

  this.drawShape = drawShape;
  this.drawConnection = drawConnection;
}

inherits(Renderer, BaseRenderer);

Renderer.$inject = [
  'config.Renderer',
  'eventBus',
  'pathMap',
  'styles',
  'textRenderer',
  'canvas',
];
