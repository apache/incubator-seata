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
package io.seata.spring.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * @author chusen
 * @emal chusen12@163.com
 */
public class AnnotationGlobalTransactionSource extends AbstractFallbackGlobalTransactionSource implements Serializable {


    private final SpringGlobalTransactionAnnotationParser transactionAnnotationParser;

    public AnnotationGlobalTransactionSource() {
        this.transactionAnnotationParser = new SpringGlobalTransactionAnnotationParser();
    }


    /**
     * find global transactional annotation on the method
     * @param method
     * @return
     */
    @Override
    protected Annotation findTransactionAnnotation(Method method) {
        Collection<? extends Annotation> annotations = transactionAnnotationParser.parseGlobalTransactionAnnotation(method);
        return annotations == null ? null : annotations.stream().findFirst().orElse(null);
    }


    /**
     * find global transactional annotation on the targetClass
     * @param targetClass
     * @return
     */
    @Override
    protected Annotation findTransactionAnnotation(Class<?> targetClass) {
        Collection<? extends Annotation> annotations = transactionAnnotationParser.parseGlobalTransactionAnnotation(targetClass);
        return annotations == null ? null : annotations.stream().findFirst().orElse(null);
    }
}
