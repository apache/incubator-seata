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

import inherits from 'inherits-browser';

import CommandInterceptor from 'diagram-js/lib/command/CommandInterceptor';

export default function ReplaceConnectionBehavior(injector, modeling, rules) {
  injector.invoke(CommandInterceptor, this);

  this.preExecute('connection.reconnect', (context) => {
    const { connection } = context;
    const source = context.newSource || connection.source;
    const target = context.newTarget || connection.target;
    const waypoints = connection.waypoints.slice();

    const allowed = rules.allowed('connection.reconnect', {
      connection,
      source,
      target,
    });

    if (!allowed || allowed.type === connection.type) {
      return;
    }

    context.connection = modeling.connect(source, target, {
      type: allowed.type,
      waypoints,
    });

    modeling.removeConnection(connection);
  }, true);
}

inherits(ReplaceConnectionBehavior, CommandInterceptor);

ReplaceConnectionBehavior.$inject = [
  'injector',
  'modeling',
  'rules',
];
