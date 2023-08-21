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

import io.seata.saga.proctrl.ProcessContext;

/**
 * Expression structure resolver
 *
 * @author ptyin
 */
public interface ExpressionResolver {
    Expression getExpression(String expressionStr);

    <T extends Expression> Object getDefaultElContext(ProcessContext context, Class<T> expressionClass);

    <T extends Expression> Object getStatusEvaluationElContext(ProcessContext context, Class<T> expressionClass);

    ExpressionFactoryManager getExpressionFactoryManager();

    void setExpressionFactoryManager(ExpressionFactoryManager expressionFactoryManager);
}
