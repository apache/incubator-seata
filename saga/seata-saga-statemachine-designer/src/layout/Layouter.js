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

import BaseLayouter from 'diagram-js/lib/layout/BaseLayouter';

import {
  getMid, getOrientation,
} from 'diagram-js/lib/layout/LayoutUtil';

import { assign } from 'min-dash';

const ADDITIONAL_WAYPOINT_DISTANCE = 20;

export default function Layouter(connectionDocking) {
  this.connectionDocking = connectionDocking;
}

inherits(Layouter, BaseLayouter);

Layouter.$inject = ['connectionDocking'];

function getConnectionDocking(point, shape) {
  return point ? (point.original || point) : getMid(shape);
}

Layouter.prototype.layoutConnection = function (connection, hints) {
  const { connectionDocking } = this;

  if (!hints) {
    hints = {};
  }

  const source = hints.source || connection.source;
  const target = hints.target || connection.target;
  let waypoints = hints.waypoints || connection.waypoints || [];
  let { connectionStart } = hints;
  let { connectionEnd } = hints;
  const orientation = getOrientation(source, target);

  if (!connectionStart) {
    connectionStart = getConnectionDocking(waypoints[0], source);
  }

  if (!connectionEnd) {
    connectionEnd = getConnectionDocking(waypoints[waypoints.length - 1], target);
  }
  waypoints = [connectionStart, connectionEnd];

  const croppedWaypoints = connectionDocking.getCroppedWaypoints(
    assign({}, connection, {
      waypoints,
    }),
    source,
    target,
  );

  connectionEnd = croppedWaypoints.pop();

  const additionalWaypoint = {
    x: connectionEnd.x,
    y: connectionEnd.y,
  };

  if (orientation.includes('bottom')) {
    additionalWaypoint.y += ADDITIONAL_WAYPOINT_DISTANCE;
  } else if (orientation.includes('top')) {
    additionalWaypoint.y -= ADDITIONAL_WAYPOINT_DISTANCE;
  } else if (orientation.includes('right')) {
    additionalWaypoint.x += ADDITIONAL_WAYPOINT_DISTANCE;
  } else {
    additionalWaypoint.x -= ADDITIONAL_WAYPOINT_DISTANCE;
  }

  waypoints = croppedWaypoints.concat([additionalWaypoint, connectionEnd]);

  return waypoints;
};
