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
import { domify, query } from 'min-dom';
import { innerSVG } from 'tiny-svg';
import Diagram from 'diagram-js';

import AlignElementsModule from 'diagram-js/lib/features/align-elements';
import AttachSupport from 'diagram-js/lib/features/attach-support';
import AutoScrollModule from 'diagram-js/lib/features/auto-scroll';
import BendpointsModule from 'diagram-js/lib/features/bendpoints';
import ConnectModule from 'diagram-js/lib/features/connect';
import ContextPadModule from 'diagram-js/lib/features/context-pad';
import ConnectPreviewModule from 'diagram-js/lib/features/connection-preview';
import CreateModule from 'diagram-js/lib/features/create';
import EditorActionsModule from 'diagram-js/lib/features/editor-actions';
import GridSnappingModule from 'diagram-js/lib/features/grid-snapping';
import KeyboardModule from 'diagram-js/lib/features/keyboard';
import KeyboardMoveModule from 'diagram-js/lib/navigation/keyboard-move';
import KeyboardMoveSelectionModule from 'diagram-js/lib/features/keyboard-move-selection';
import LassoToolModule from 'diagram-js/lib/features/lasso-tool';
import MoveCanvasModule from 'diagram-js/lib/navigation/movecanvas';
import MoveModule from 'diagram-js/lib/features/move';
import OutlineModule from 'diagram-js/lib/features/outline';
import PaletteModule from 'diagram-js/lib/features/palette';
import ResizeModule from 'diagram-js/lib/features/resize';
import RulesModule from 'diagram-js/lib/features/rules';
import SelectionModule from 'diagram-js/lib/features/selection';
import SnappingModule from 'diagram-js/lib/features/snapping';
import ZoomScrollModule from 'diagram-js/lib/navigation/zoomscroll';

import GridModule from 'diagram-js-grid';

import Layout from './layout';
import Modeling from './modeling';
import Providers from './providers';
import Render from './render';

import 'diagram-js/assets/diagram-js.css';
import '@bpmn-io/properties-panel/assets/properties-panel.css';
import 'bpmn-font/dist/css/bpmn.css';
import './index.css';

/**
 * Seata Saga Designer editor constructor
 *
 * @param { { container: Element, additionalModules?: Array<any> } } options
 *
 * @return {Diagram}
 */
export default function Editor(options) {
  this.container = this.createContainer();
  this.init(this.container, options);
}

// Make Editor inherit from diagram-js/Diagram
inherits(Editor, Diagram);

// Add modules for the Editor
Editor.prototype.modules = [
  // Customized modules
  Layout,
  Modeling,
  Providers,
  Render,

  // Built-in modules
  AlignElementsModule,
  AttachSupport,
  AutoScrollModule,
  BendpointsModule,
  ConnectModule,
  ConnectPreviewModule,
  ContextPadModule,
  CreateModule,
  GridModule,
  GridSnappingModule,
  EditorActionsModule,
  KeyboardModule,
  KeyboardMoveModule,
  KeyboardMoveSelectionModule,
  LassoToolModule,
  MoveCanvasModule,
  MoveModule,
  OutlineModule,
  PaletteModule,
  ResizeModule,
  RulesModule,
  SelectionModule,
  SnappingModule,
  ZoomScrollModule,
];

/**
 * Create a container to mount
 * @returns {HTMLElement}
 */
Editor.prototype.createContainer = function () {
  return domify(
    '<div class="statemachine-designer-container" style="width: 100%; height: 100%"></div>',
  );
};

/**
 * A utility function to expose the event bus
 */
Editor.prototype.emit = function (type, event) {
  return this.get('eventBus').fire(type, event);
};

/**
 * Detach the editor from the actual container
 */
Editor.prototype.detach = function () {
  const { container } = this;
  const { parentNode } = container;

  if (!parentNode) {
    return;
  }

  this.emit('detach', {});

  parentNode.removeChild(container);
};

/**
 * Attach the editor to a specific container
 */
Editor.prototype.attachTo = function (parentNode) {
  if (!parentNode) {
    throw new Error('parentNode required');
  }

  // ensure we detach from the
  // previous, old parent
  this.detach();

  parentNode.appendChild(this.container);

  this.emit('attach', {});

  this.get('canvas').resized();
};

/**
 * Initialize the editor
 */
Editor.prototype.init = function (container, options) {
  const {
    additionalModules,
    canvas,
    ...additionalOptions
  } = options;

  const baseModules = options.modules || this.modules;

  const modules = [
    ...baseModules,
    ...(additionalModules || []),
  ];

  const diagramOptions = {
    ...additionalOptions,
    canvas: {
      ...canvas,
      container,
    },
    modules,
  };

  // invoke diagram constructor
  Diagram.call(this, diagramOptions);

  if (options && options.container) {
    this.attachTo(options.container);
  }

  this.get('eventBus').fire('editor.attached');
};

/**
 * Clear the editor, removing all contents.
 */
Editor.prototype.clear = function () {
  Diagram.prototype.clear.call(this);
};

/**
 * Import diagram from JSON definitions.
 */
Editor.prototype.import = function (definitions) {
  this.clear();
  this.get('sagaImporter').import(definitions);
};

/**
 * Export diagram to JSON definitions
 */
Editor.prototype.export = function () {
  return this.get('sagaExporter').export();
};

/**
 * Export diagram to a SVG figure
 */
Editor.prototype.exportSvg = function () {
  const eventBus = this.get('eventBus');
  eventBus.fire('saveSVG.start');

  let svg;
  let
    err;

  try {
    const canvas = this.get('canvas');

    const contentNode = canvas.getActiveLayer();
    // eslint-disable-next-line no-underscore-dangle
    const defsNode = query('defs', canvas._svg);

    const contents = innerSVG(contentNode);
    const defs = defsNode ? `<defs>${innerSVG(defsNode)}</defs>` : '';

    const bbox = contentNode.getBBox();

    svg = '<?xml version="1.0" encoding="utf-8"?>\n'
      + '<!-- created with seata-saga-statemachine-designer / https://seata.io -->\n'
      + '<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">\n'
      + '<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
      + `width="${bbox.width}" height="${bbox.height}" `
      + `viewBox="${bbox.x} ${bbox.y} ${bbox.width} ${bbox.height}" version="1.1">${
        defs}${contents
      }</svg>`;
  } catch (e) {
    err = e;
  }

  eventBus.fire('saveSVG.done', {
    error: err,
    svg,
  });

  if (err) {
    throw err;
  }

  return svg;
};
