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
package io.seata.spring.aot;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

/**
 * The seata TCC mode runtime hints registrar
 *
 * @author wang.liang
 */
class SeataTCCModeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        ReflectionHints reflectionHints = hints.reflection();

        // Register following classes for HSFRemotingParser#isHsf
        AotUtils.registerTypes(reflectionHints,
                AotUtils.EMPTY_MEMBER_CATEGORIES,
                "com.taobao.hsf.app.api.util.HSFApiConsumerBean",
                "com.taobao.hsf.app.api.util.HSFApiProviderBean",
                "com.taobao.hsf.app.spring.util.HSFSpringConsumerBean",
                "com.taobao.hsf.app.spring.util.HSFSpringProviderBean"
        );
    }

}
