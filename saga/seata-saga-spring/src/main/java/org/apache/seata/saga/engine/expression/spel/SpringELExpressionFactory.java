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

import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.AccessException;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * SpringELExpression factory
 *
 */
public class SpringELExpressionFactory implements ExpressionFactory {

    ExpressionParser parser = new SpelExpressionParser();
    ApplicationContext applicationContext;

    public SpringELExpressionFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Expression createExpression(String expression) {
        org.springframework.expression.Expression defaultExpression = parser.parseExpression(expression);
        EvaluationContext evaluationContext = ((SpelExpression)defaultExpression).getEvaluationContext();
        ((StandardEvaluationContext)evaluationContext).setBeanResolver(new AppContextBeanResolver());
        return new SpringELExpression(defaultExpression);
    }

    private class AppContextBeanResolver implements BeanResolver {

        @Override
        public Object resolve(EvaluationContext context, String beanName) throws AccessException {
            return applicationContext.getBean(beanName);
        }

    }
}
