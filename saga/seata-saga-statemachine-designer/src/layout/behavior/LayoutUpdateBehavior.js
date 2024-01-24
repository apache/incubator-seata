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

import inherits from 'inherits-browser';

import CommandInterceptor from 'diagram-js/lib/command/CommandInterceptor';

export default function LayoutUpdateBehavior(injector) {
  injector.invoke(CommandInterceptor, this);
  const self = this;

  function updateBounds(context) {
    const { shape } = context;
    self.updateBounds(shape);
  }

  this.executed(['shape.create', 'shape.move', 'shape.resize'], updateBounds, true);
  this.reverted(['shape.create', 'shape.move', 'shape.resize'], updateBounds, true);

  function updateConnectionWaypoints(context) {
    self.updateConnectionWaypoints(context);
  }

  this.executed([
    'connection.create',
    'connection.layout',
    'connection.move',
    'connection.updateWaypoints',
  ], updateConnectionWaypoints, true);

  this.reverted([
    'connection.create',
    'connection.layout',
    'connection.move',
    'connection.updateWaypoints',
  ], updateConnectionWaypoints, true);

  function updateConnectionSourceTarget(context) {
    self.updateConnectionSourceTarget(context);
  }

  this.executed(['connection.create', 'connection.reconnect'], updateConnectionSourceTarget, true);
  this.reverted(['connection.create', 'connection.reconnect'], updateConnectionSourceTarget, true);
}

inherits(LayoutUpdateBehavior, CommandInterceptor);

LayoutUpdateBehavior.$inject = ['injector'];

LayoutUpdateBehavior.prototype.updateBounds = function (shape) {
  const { businessObject } = shape;
  const { bounds } = businessObject.style;

  // update bounds
  assign(bounds, {
    x: shape.x,
    y: shape.y,
    width: shape.width,
    height: shape.height,
  });
};

LayoutUpdateBehavior.prototype.updateConnectionWaypoints = function (context) {
  const { connection } = context;
  const { businessObject } = connection;
  const { waypoints } = businessObject.style;

  assign(waypoints, connection.waypoints);
};

LayoutUpdateBehavior.prototype.updateConnectionSourceTarget = function (context) {
  const { connection } = context;
  const { businessObject } = connection;
  const { source, newSource, target, newTarget } = context;

  businessObject.style.source = newSource || source;
  businessObject.style.target = newTarget || target;
};
