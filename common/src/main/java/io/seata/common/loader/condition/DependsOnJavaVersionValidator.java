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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 依赖Java版本校验器
 *
 * @author wang.liang
 */
public class DependsOnJavaVersionValidator implements IDependsOnValidator {

    @Override
    public void validate(Class<?> serviceClass, ClassLoader classLoader) throws ServiceDependencyException {
        // 获取注解`@DependsOnJavaVersion`的信息，并判断Java版本是否符合
        DependsOnJavaVersion dependsOnJavaVersion = serviceClass.getAnnotation(DependsOnJavaVersion.class);
        if (dependsOnJavaVersion == null) {
            return;
        }

        // 获取依赖的Java版本范围
        int dependsOnMinJavaVersion = (int)(dependsOnJavaVersion.min() * 100);
        int dependsOnMaxJavaVersion = (int)(dependsOnJavaVersion.max() * 100);
        // 默认包含所有小版本处理
        dependsOnMaxJavaVersion = this.handleDependsOnMaxJavaVersion(dependsOnMaxJavaVersion);
        // 判断依赖的Java版本
        if (dependsOnMinJavaVersion > 0 || dependsOnMaxJavaVersion > 0) {
            int javaVersion = getJavaVersionAsInt();
            if (dependsOnMinJavaVersion > 0 && javaVersion < dependsOnMinJavaVersion) {
                throw new ServiceDependencyException("java version is less than v" + dependsOnJavaVersion.min());
            }
            if (dependsOnMaxJavaVersion > 0 && javaVersion > dependsOnMaxJavaVersion) {
                throw new ServiceDependencyException("java version is greater than v" + dependsOnJavaVersion.max());
            }
        }
    }

    private int getJavaVersionAsInt() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            return 0;
        }

        String str = javaVersion;

        final String regex = "^[0-9]{1,2}(\\.[0-9]{1,2})?";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            str = matcher.group(0);
        } else {
            return 0;
        }

        return (int)(Float.parseFloat(str) * 100);
    }

    private int handleDependsOnMaxJavaVersion(int dependsOnMaxJavaVersion) {
        if (dependsOnMaxJavaVersion > 0) {
            if (dependsOnMaxJavaVersion < 190) {
                if (dependsOnMaxJavaVersion % 10 == 0) {
                    dependsOnMaxJavaVersion += 9;
                }
            } else {
                if (dependsOnMaxJavaVersion % 100 == 0) {
                    dependsOnMaxJavaVersion += 99;
                }
            }
        }
        return dependsOnMaxJavaVersion;
    }
}
