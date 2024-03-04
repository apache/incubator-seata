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

import State from './State';

export default class Choice extends State {
  importJson(json) {
    super.importJson(json);
    if (json.edge) {
      this.Choices.forEach((choice) => {
        if (json.edge[choice.Next]) {
          json.edge[choice.Next].Expression = choice.Expression;
        }
      });
      if (json.edge[this.Default]) {
        json.edge[this.Default].Default = true;
      }
    }
    delete this.Choices;
    delete this.Default;
  }
}

Choice.prototype.Type = 'Choice';

Choice.prototype.THUMBNAIL_CLASS = 'bpmn-icon-gateway-xor';

Choice.prototype.DEFAULT_SIZE = {
  width: 50,
  height: 50,
};
