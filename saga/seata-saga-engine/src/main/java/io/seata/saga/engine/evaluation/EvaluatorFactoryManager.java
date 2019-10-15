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
package io.seata.saga.engine.evaluation;

import io.seata.common.util.StringUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluator Factory Manager
 *
 * @see EvaluatorFactory
 * @see Evaluator
 * @author lorne.cl
 */
public class EvaluatorFactoryManager {

    public static final String EVALUATOR_TYPE_DEFAULT = "Default";

    private Map<String, EvaluatorFactory> evaluatorFactoryMap = new ConcurrentHashMap<>();

    public EvaluatorFactory getEvaluatorFactory(String type){

        if(StringUtils.isBlank(type)){
            type = EVALUATOR_TYPE_DEFAULT;
        }
        return this.evaluatorFactoryMap.get(type);
    }

    public Map<String, EvaluatorFactory> getEvaluatorFactoryMap() {
        return evaluatorFactoryMap;
    }

    public void setEvaluatorFactoryMap(Map<String, EvaluatorFactory> evaluatorFactoryMap) {
        this.evaluatorFactoryMap.putAll(evaluatorFactoryMap);
    }

    public void putEvaluatorFactory(String type, EvaluatorFactory factory){
        this.evaluatorFactoryMap.put(type, factory);
    }
}