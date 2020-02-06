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
package io.seata.spring.boot.autoconfigure.provider;

import io.seata.common.holder.ObjectHolder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;

/**
 * @author xingfudeshi@gmail.com
 * The type spring application context provider
 */
public class SpringApplicationContextProvider implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT, applicationContext);
    }
}
