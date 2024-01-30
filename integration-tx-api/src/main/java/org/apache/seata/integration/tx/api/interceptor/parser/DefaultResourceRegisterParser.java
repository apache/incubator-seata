/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.integration.tx.api.interceptor.parser;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;


public class DefaultResourceRegisterParser {

    protected static List<RegisterResourceParser> allRegisterResourceParsers = new ArrayList<>();

    public void registerResource(Object target, String beanName) {
        for (RegisterResourceParser registerResourceParser : allRegisterResourceParsers) {
            registerResourceParser.registerResource(target, beanName);
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
