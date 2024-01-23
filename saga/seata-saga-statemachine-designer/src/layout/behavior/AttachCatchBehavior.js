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
import { is } from '../../utils';

const LOW_PRIORITY = 500;

function shouldUpdate(shape, host) {
  return is(shape, 'Catch') && host;
}

export default function AttachEventBehavior(injector) {
  injector.invoke(CommandInterceptor, this);
  this.postExecuted('element.updateAttachment', LOW_PRIORITY, ({ context }) => {
    const { shape, oldHost, newHost } = context;

    if (shouldUpdate(shape, newHost)) {
      delete oldHost?.businessObject.Catch;
      newHost.businessObject.Catch = shape.businessObject;
    }
  });
}

inherits(AttachEventBehavior, CommandInterceptor);

AttachEventBehavior.$inject = ['injector'];
