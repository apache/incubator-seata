/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.saga.engine.expression.spel;

import org.apache.seata.saga.engine.expression.ELExpression;

/**
 * Expression base on Spring EL
 *
 */
public class SpringELExpression implements ELExpression {

    private org.springframework.expression.Expression expression;

    public SpringELExpression(org.springframework.expression.Expression expression) {
        this.expression = expression;
    }

    @Override
    public Object getValue(Object elContext) {
        return expression.getValue(elContext);
    }

    @Override
    public void setValue(Object value, Object elContext) {
        expression.setValue(elContext, value);
    }

    @Override
    public String getExpressionString() {
        return expression.getExpressionString();
    }
}
