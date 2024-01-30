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


public class DefaultTargetClassParser implements TargetClassParser {

    protected static List<TargetClassParser> allTargetClassParsers = new ArrayList<>();


    private static class SingletonHolder {
        private static final DefaultTargetClassParser INSTANCE = new DefaultTargetClassParser();
    }

    public static DefaultTargetClassParser get() {
        return DefaultTargetClassParser.SingletonHolder.INSTANCE;
    }

    protected DefaultTargetClassParser() {
        initTargetClassParser();
    }

    /**
     * init parsers
     */
    protected void initTargetClassParser() {
        List<TargetClassParser> targetClassParsers = EnhancedServiceLoader.loadAll(TargetClassParser.class);
        if (CollectionUtils.isNotEmpty(targetClassParsers)) {
            allTargetClassParsers.addAll(targetClassParsers);
        }
    }

    @Override
    public Class<?> findTargetClass(Object target) throws Exception {
        for (TargetClassParser targetClassParser : allTargetClassParsers) {
            Class<?> result = targetClassParser.findTargetClass(target);
            if (result != null) {
                return result;
            }
        }
        return target.getClass();
    }

    @Override
    public Class<?>[] findInterfaces(Object target) throws Exception {
        for (TargetClassParser targetClassParser : allTargetClassParsers) {
            Class<?>[] result = targetClassParser.findInterfaces(target);
            if (result != null) {
                return result;
            }
        }
        return target.getClass().getInterfaces();
    }
}
