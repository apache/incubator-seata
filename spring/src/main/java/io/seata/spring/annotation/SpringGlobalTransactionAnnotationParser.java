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

import org.springframework.core.annotation.AnnotatedElementUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

/**
 * scan GlobalTransactional and GlobalLock
 *
 * @author chusen
 * @email chusen12@163.com
 */
public class SpringGlobalTransactionAnnotationParser implements Serializable {


    private static final Set<Class<? extends Annotation>> GLOBAL_TRANSACTION_ANNOTATIONS = new LinkedHashSet<>(2);

    static {
        // When GlobalTransactional and GlobalLock are on methods at the same time, @GlobalTransactional shall prevail
        GLOBAL_TRANSACTION_ANNOTATIONS.add(GlobalTransactional.class);
        GLOBAL_TRANSACTION_ANNOTATIONS.add(GlobalLock.class);
    }

    public Collection<? extends Annotation> parseGlobalTransactionAnnotation(Class<?> type) {
        return parseGlobalTransactionAnnotations(type);
    }


    public Collection<? extends Annotation> parseGlobalTransactionAnnotation(Method method) {
        return parseGlobalTransactionAnnotations(method);
    }


    private Collection<? extends Annotation> parseGlobalTransactionAnnotations(AnnotatedElement ae) {
        Collection<? extends Annotation> annotations = parseGlobalTransactionAnnotations(ae, false);
        if (annotations != null && annotations.size() > 1) {
            // More than one annotation found -> local declarations override interface-declared ones...
            Collection<? extends Annotation> localAnnotation = parseGlobalTransactionAnnotations(ae, true);
            if (localAnnotation != null) {
                return localAnnotation;
            }
        }
        return annotations;
    }


    private Collection<? extends Annotation> parseGlobalTransactionAnnotations(AnnotatedElement ae, boolean localOnly) {
        Collection<Annotation> anns = new LinkedHashSet<>(2);
        if (localOnly) {
            GLOBAL_TRANSACTION_ANNOTATIONS.forEach(annotation -> anns.addAll(AnnotatedElementUtils.getAllMergedAnnotations(ae, annotation)));
        } else {
            GLOBAL_TRANSACTION_ANNOTATIONS.forEach(annotation -> anns.addAll(AnnotatedElementUtils.findAllMergedAnnotations(ae, annotation)));
        }
        return anns.isEmpty() ? null : anns;
    }


}
