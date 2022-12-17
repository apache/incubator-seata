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
package io.seata.commonapi.interceptor.parser;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class DefaultResourceRegisterParser {

    protected static List<RegisterResourceParser> allRegisterResourceParsers = new ArrayList<>();

    public void registerResource(Object target) {
        for (RegisterResourceParser registerResourceParser : allRegisterResourceParsers) {
            registerResourceParser.registerResource(target);
        }
    }

    private static class SingletonHolder {
        private static final DefaultResourceRegisterParser INSTANCE = new DefaultResourceRegisterParser();
    }

    public static DefaultResourceRegisterParser get() {
        return DefaultResourceRegisterParser.SingletonHolder.INSTANCE;
    }

    protected DefaultResourceRegisterParser() {
        initResourceRegisterParser();
    }

    /**
     * init parsers
     */
    protected void initResourceRegisterParser() {
        List<RegisterResourceParser> registerResourceParsers = EnhancedServiceLoader.loadAll(RegisterResourceParser.class);
        if (CollectionUtils.isNotEmpty(registerResourceParsers)) {
            allRegisterResourceParsers.addAll(registerResourceParsers);
        }
    }


}