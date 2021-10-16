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

/**
 * 依赖类校验器
 *
 * @author wang.liang
 */
public class DependsOnClassValidator implements IDependsOnValidator {

    @Override
    public void validate(Class<?> serviceClass, ClassLoader classLoader) throws ServiceDependencyException {
        // 获取注解`@DependsOnClass`的信息，并判断类是否存在
        try {
            // 在java11之前的版本中，如果设置的value中有类不存在，则会在这行代码直接抛出ArrayStoreException异常
            DependsOnClass dependsOnClass = serviceClass.getAnnotation(DependsOnClass.class);
            if (dependsOnClass == null) {
                return;
            }

            // 在java11及以上版本中，必须访问过一次注解的属性值，才会抛出TypeNotPresentException异常
            @SuppressWarnings("all")
            Class<?>[] dependsOnClasses = dependsOnClass.value();

            // 根据类名判断
            String[] dependsOnClassNames = dependsOnClass.name();
            for (String dependsOnClassName : dependsOnClassNames) {
                Class.forName(dependsOnClassName, true, classLoader);
            }
        } catch (ArrayStoreException | TypeNotPresentException | ClassNotFoundException e) {
            throw new ServiceDependencyException("the depends on classes is not found", e);
        }
    }
}
