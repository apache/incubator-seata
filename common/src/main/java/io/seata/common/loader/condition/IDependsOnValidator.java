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
package io.seata.common.loader.condition;

import io.seata.common.loader.IServiceLoaderValidator;

/**
 * 依赖校验器接口
 *
 * @author wang.liang
 */
public interface IDependsOnValidator extends IServiceLoaderValidator {

    /**
     * 校验注解信息
     *
     * @param serviceClass 服务类型
     * @param classLoader  类加载器
     * @throws ServiceDependencyException 依赖无效或不匹配时，请抛出该异常
     */
    @Override
    void validate(Class<?> serviceClass, ClassLoader classLoader) throws ServiceDependencyException;
}
