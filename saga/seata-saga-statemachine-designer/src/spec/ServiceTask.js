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

import TaskState from './TaskState';

export default class ServiceTask extends TaskState {
  constructor() {
    super();
    this.ServiceName = '';
    this.ServiceMethod = '';
  }
}

ServiceTask.prototype.Type = 'ServiceTask';

ServiceTask.prototype.THUMBNAIL_CLASS = 'bpmn-icon-service-task';

ServiceTask.prototype.DEFAULT_SIZE = {
  width: 100,
  height: 80,
};
