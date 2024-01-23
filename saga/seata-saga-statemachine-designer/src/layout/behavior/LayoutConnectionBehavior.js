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

import CommandInterceptor from 'diagram-js/lib/command/CommandInterceptor';

import {
  asTRBL,
  getMid,
  getOrientation,
} from 'diagram-js/lib/layout/LayoutUtil';

import {
  assign,
  forEach,
} from 'min-dash';

const LOW_PRIORITY = 500;

// helpers //////////

function getConnectionHints(source, target, orientation) {
  const connectionStart = getMid(source);
  const connectionEnd = getMid(target);

  if (orientation.includes('bottom')) {
    connectionStart.y = source.y;
    connectionEnd.y = target.y + target.height;
  } else if (orientation.includes('top')) {
    connectionStart.y = source.y + source.height;
    connectionEnd.y = target.y;
  } else if (orientation.includes('right')) {
    connectionStart.x = source.x;
    connectionEnd.x = target.x + target.width;
  } else {
    connectionStart.x = source.x + source.width;
    connectionEnd.x = target.x;
  }

  return {
    connectionStart,
    connectionEnd,
  };
}

/**
 * Get connections start and end based on number of connections and
 * orientation.
 *
 * @param {Array<djs.model.Connection>} connections
 * @param {djs.model.Shape} target
 * @param {string} orientation
 *
 * @returns {Array<Object>}
 */
function getConnectionsStartEnd(connections, target, orientation) {
  return connections.map(
    (connection, index) => {
      const { source } = connection;
      const sourceMid = getMid(source);
      const sourceTrbl = asTRBL(source);
      const targetTrbl = asTRBL(target);

      const { length } = connections;

      if (orientation.includes('bottom')) {
        return {
          start: {
            x: sourceMid.x,
            y: sourceTrbl.top,
          },
          end: {
            x: targetTrbl.left + (target.width / (length + 1)) * (index + 1),
            y: targetTrbl.bottom,
          },
        };
      } if (orientation.includes('top')) {
        return {
          start: {
            x: sourceMid.x,
            y: sourceTrbl.bottom,
          },
          end: {
            x: targetTrbl.left + (target.width / (length + 1)) * (index + 1),
            y: targetTrbl.top,
          },
        };
      } if (orientation.includes('right')) {
        return {
          start: {
            x: sourceTrbl.left,
            y: sourceMid.y,
          },
          end: {
            x: targetTrbl.right,
            y: targetTrbl.top + (target.height / (length + 1)) * (index + 1),
          },
        };
      }
      return {
        start: {
          x: sourceTrbl.right,
          y: sourceMid.y,
        },
        end: {
          x: targetTrbl.left,
          y: targetTrbl.top + (target.height / (length + 1)) * (index + 1),
        },
      };
    },
  );
}

/**
 * Get connections by orientation.
 *
 * @param {djs.model.shape} target
 * @param {Array<djs.model.Connection>} connections
 *
 * @returns {Object}
 */
function getConnectionByOrientation(target, connections) {
  const incomingConnectionsByOrientation = {};

  connections.forEach((incoming) => {
    const orientation = getOrientation(incoming.source, target).split('-').shift();

    if (!incomingConnectionsByOrientation[orientation]) {
      incomingConnectionsByOrientation[orientation] = [];
    }

    incomingConnectionsByOrientation[orientation].push(incoming);
  });

  return incomingConnectionsByOrientation;
}

function isSameOrientation(orientationA, orientationB) {
  return orientationA
    && orientationB
    && orientationA.split('-').shift() === orientationB.split('-').shift();
}

function sortConnections(connections, orientation) {
  let axis;

  if (orientation.includes('top') || orientation.includes('bottom')) {
    axis = 'x';
  } else {
    axis = 'y';
  }

  return connections.sort((a, b) => {
    return getMid(a.source)[axis] - getMid(b.source)[axis];
  });
}

export default function LayoutConnectionBehavior(injector, layouter, modeling) {
  injector.invoke(CommandInterceptor, this);

  // specify connection start and end on connection create
  this.preExecute([
    'connection.create',
    'connection.reconnect',
  ], (context) => {
    const source = context.newSource || context.source;
    const target = context.newTarget || context.target;

    const orientation = getOrientation(source, target);

    if (!context.hints) {
      context.hints = {};
    }

    assign(context.hints, getConnectionHints(source, target, orientation));
  }, true);

  /**
   * Update incoming connections.
   *
   * @param {djs.model.Shape} target
   * @param {Array<djs.model.Connection>} [connection]
   * @param {string} [orientation]
   */
  function updateConnections(target, connection, orientation) {
    // (1) get connection
    if (!connection) {
      connection = target.incoming;
    }

    let incomingConnectionsByOrientation = {};

    // (2) get connections per orientation
    if (orientation) {
      incomingConnectionsByOrientation[orientation] = connection;
    } else {
      incomingConnectionsByOrientation = getConnectionByOrientation(target, connection);
    }

    // (3) update connections per orientation
    forEach(
      incomingConnectionsByOrientation,
      (connections, ot) => {
        // (3.1) sort connections
        connections = sortConnections(connections, ot);

        // (3.2) get new connection start and end
        const connectionStartEnd = getConnectionsStartEnd(connections, target, ot);

        // (3.3) update connections
        connections.forEach((conn, index) => {
          const connectionStart = connectionStartEnd[index].start;
          const connectionEnd = connectionStartEnd[index].end;

          const waypoints = layouter.layoutConnection(conn, {
            connectionStart,
            connectionEnd,
          });

          modeling.updateWaypoints(conn, waypoints);
        });
      },
    );
  }

  // update connections on connection create and delete
  // update connections of new target on connection reconnect
  this.postExecuted([
    'connection.create',
    'connection.delete',
    'connection.reconnect',
  ], (context) => {
    const { connection } = context;
    const source = connection.source || context.source;
    const target = connection.target || context.target;

    const orientation = getOrientation(source, target);

    // update all connections with same orientation
    const connections = target.incoming.filter((incoming) => {
      const incomingOrientation = getOrientation(incoming.source, incoming.target);

      return isSameOrientation(incomingOrientation, orientation);
    });

    if (!connections.length) {
      return;
    }

    updateConnections(target, connections, orientation);
  }, true);

  // update connections of old target on connection reconnect
  this.preExecute('connection.reconnect', (context) => {
    const { connection } = context;
    const { source } = connection;
    const { target } = connection;

    const orientation = getOrientation(source, target);

    // update all connections with same orientation except reconnected
    const connections = target.incoming.filter((incoming) => {
      const incomingOrientation = getOrientation(incoming.source, incoming.target);

      return incoming !== connection
        && isSameOrientation(incomingOrientation, orientation);
    });

    if (!connections.length) {
      return;
    }

    updateConnections(target, connections, orientation);
  }, true);

  // update connections on elements move
  this.postExecuted('elements.move', LOW_PRIORITY, (context) => {
    const { shapes } = context;
    const { closure } = context;
    const { enclosedConnections } = closure;

    shapes.forEach((shape) => {
      // (1) update incoming connections
      const incomingConnections = shape.incoming.filter((incoming) => {
        return !enclosedConnections[incoming.id];
      });

      if (incomingConnections.length) {
        updateConnections(shape, incomingConnections);
      }

      // (2) update outgoing connections
      shape.outgoing.forEach((outgoing) => {
        if (enclosedConnections[outgoing.id]) {
          return;
        }

        updateConnections(outgoing.target);
      });
    });
  }, true);
}

LayoutConnectionBehavior.$inject = [
  'injector',
  'layouter',
  'modeling',
  'rules',
];

inherits(LayoutConnectionBehavior, CommandInterceptor);
