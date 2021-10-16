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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务依赖的Jar及其版本
 *
 * @author wang.liang
 * @see DependsOnJarVersionValidator
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DependsOnJarVersion {

    /**
     * 依赖的Jar包名称数组<br>
     * 由于部分jar变更过名字，所以可设置多个
     *
     * @return the jar names
     */
    String[] name();

    /**
     * 依赖的Jar包最小版本号
     *
     * @return the min version
     */
    String minVersion() default "";

    /**
     * 依赖的Jar包最大版本号
     *
     * @return the min version
     */
    String maxVersion() default "";
}
