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
package io.seata.common.loader;

import io.seata.common.loader.condition.DependsOnJarVersion;

/**
 * The type Error hello 2.
 *
 * @author wang.liang
 */
@LoadLevel(name = "ErrorHello2", order = Integer.MAX_VALUE)
@DependsOnJarVersion(name = "slf4j-api", maxVersion = "0.0.1")
public class ErrorHello2 implements Hello {

    @Override
    public String say() {
        return "error hello2!";
    }
}
