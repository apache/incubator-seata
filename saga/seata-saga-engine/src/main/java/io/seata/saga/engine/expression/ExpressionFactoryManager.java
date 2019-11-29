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
package io.seata.saga.engine.expression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.util.StringUtils;

/**
 * Expression factory manager
 *
 * @author lorne.cl
 */
public class ExpressionFactoryManager {

    public static final String DEFAULT_EXPRESSION_TYPE = "Default";

    private Map<String, ExpressionFactory> expressionFactoryMap = new ConcurrentHashMap<>();

    public ExpressionFactory getExpressionFactory(String expressionType) {
        if (StringUtils.isBlank(expressionType)) {
            expressionType = DEFAULT_EXPRESSION_TYPE;
        }
        return expressionFactoryMap.get(expressionType);
    }

    public void setExpressionFactoryMap(Map<String, ExpressionFactory> expressionFactoryMap) {

        this.expressionFactoryMap.putAll(expressionFactoryMap);
    }

    public void putExpressionFactory(String type, ExpressionFactory factory) {
        this.expressionFactoryMap.put(type, factory);
    }
}