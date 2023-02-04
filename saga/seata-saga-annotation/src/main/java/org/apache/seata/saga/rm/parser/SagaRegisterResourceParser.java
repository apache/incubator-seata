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
package org.apache.seata.saga.rm.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.seata.saga.rm.SagaAnnotationResource;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;
import org.apache.seata.core.model.Resource;
import org.apache.seata.rm.tcc.resource.parser.TccRegisterResourceParser;


/**
 * @author leezongjie
 * @date 2022/12/17
 */
public class SagaRegisterResourceParser extends TccRegisterResourceParser {

    @Override
    protected Resource createResource(Object targetBean, Class<?> serviceClass, Method method, Annotation annotation) throws NoSuchMethodException {
        CompensationBusinessAction compensationBusinessAction = (CompensationBusinessAction) annotation;
        SagaAnnotationResource sagaAnnotationResource = new SagaAnnotationResource();
        sagaAnnotationResource.setActionName(compensationBusinessAction.name());
        sagaAnnotationResource.setTargetBean(targetBean);
        sagaAnnotationResource.setPrepareMethod(method);
        sagaAnnotationResource.setRollbackMethodName(compensationBusinessAction.compensationMethod());
        sagaAnnotationResource.setRollbackMethod(serviceClass.getMethod(compensationBusinessAction.compensationMethod(),
                compensationBusinessAction.compensationArgsClasses()));
        // set argsClasses
        sagaAnnotationResource.setRollbackArgsClasses(compensationBusinessAction.compensationArgsClasses());
        // set phase two method's keys
        sagaAnnotationResource.setPhaseTwoRollbackKeys(this.getTwoPhaseArgs(sagaAnnotationResource.getRollbackMethod(),
                compensationBusinessAction.compensationArgsClasses()));

        return sagaAnnotationResource;
    }

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return CompensationBusinessAction.class;
    }

}
