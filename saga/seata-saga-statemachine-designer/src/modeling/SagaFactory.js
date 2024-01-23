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

import Transition from '../spec/Transition';
import StateMachine from '../spec/StateMachine';
import ServiceTask from '../spec/ServiceTask';
import StartState from '../spec/StartState';
import ScriptTask from '../spec/ScriptTask';
import Choice from '../spec/Choice';
import ChoiceEntry from '../spec/ChoiceEntry';
import Succeed from '../spec/Succeed';
import Fail from '../spec/Fail';
import Catch from '../spec/Catch';
import ExceptionMatch from '../spec/ExceptionMatch';
import CompensationTrigger from '../spec/CompensationTrigger';
import SubStateMachine from '../spec/SubStateMachine';
import Compensation from '../spec/Compensation';

export default function SagaFactory() {
  const typeToSpec = new Map();
  typeToSpec.set('Transition', Transition);
  typeToSpec.set('StartState', StartState);
  typeToSpec.set('StateMachine', StateMachine);
  typeToSpec.set('ServiceTask', ServiceTask);
  typeToSpec.set('ScriptTask', ScriptTask);
  typeToSpec.set('Choice', Choice);
  typeToSpec.set('ChoiceEntry', ChoiceEntry);
  typeToSpec.set('Succeed', Succeed);
  typeToSpec.set('Fail', Fail);
  typeToSpec.set('Catch', Catch);
  typeToSpec.set('ExceptionMatch', ExceptionMatch);
  typeToSpec.set('CompensationTrigger', CompensationTrigger);
  typeToSpec.set('SubStateMachine', SubStateMachine);
  typeToSpec.set('Compensation', Compensation);
  this.typeToSpec = typeToSpec;
}

SagaFactory.prototype.create = function (type) {
  const Spec = this.typeToSpec.get(type);
  return new Spec();
};

SagaFactory.prototype.getDefaultSize = function (semantic) {
  if (semantic.DEFAULT_SIZE) {
    return semantic.DEFAULT_SIZE;
  }

  return {
    width: 100,
    height: 80,
  };
};
