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

import DiagramCommand from 'diagram-js/lib/command';
import DiagramChangeSupport from 'diagram-js/lib/features/change-support';
import DiagramRulesModule from 'diagram-js/lib/features/rules';
import DiagramSelection from 'diagram-js/lib/features/selection';

import ElementFactory from './ElementFactory';
import Modeling from './Modeling';
import SagaFactory from './SagaFactory';
import SagaRules from './SagaRules';
import SagaExporter from './SagaExporter';
import SagaImporter from './SagaImporter';

export default {
  __init__: [
    'modeling',
    'sagaImporter',
    'sagaExporter',
    'sagaFactory',
    'sagaRules',
  ],
  __depends__: [
    DiagramCommand,
    DiagramChangeSupport,
    DiagramRulesModule,
    DiagramSelection,
  ],
  elementFactory: ['type', ElementFactory],
  modeling: ['type', Modeling],
  sagaImporter: ['type', SagaImporter],
  sagaExporter: ['type', SagaExporter],
  sagaFactory: ['type', SagaFactory],
  sagaRules: ['type', SagaRules],
};
