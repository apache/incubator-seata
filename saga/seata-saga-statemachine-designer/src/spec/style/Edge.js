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

import { assign } from 'min-dash';
import BaseSpec from '../BaseSpec';
import EdgeStyle from './EdgeStyle';

export default class Edge extends BaseSpec {
  style = new EdgeStyle();

  importJson(json) {
    this.style.source = json.style.source;
    this.style.target = json.style.target;
    assign(this.style.waypoints, json.style.waypoints);
  }

  exportJson() {
    const json = assign({ style: new EdgeStyle() }, { style: { waypoints: this.style.waypoints } });
    json.style.source = this.style.source.businessObject.Name;
    json.style.target = this.style.target.businessObject.Name;
    return json;
  }
}
