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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import static io.seata.common.loader.EnhancedServiceLoader.SEATA_DIRECTORY;
import static io.seata.common.loader.EnhancedServiceLoader.SERVICES_DIRECTORY;

/**
 * The seata /META-INF/services runtime hints registrar
 *
 * @author wang.liang
 */
class SeataServicesRuntimeHints implements RuntimeHintsRegistrar {

    private static final Set<String> OTHER_SERVICES = new HashSet<>();

    static {
        OTHER_SERVICES.add("com.alibaba.dubbo.rpc.Filter");
        OTHER_SERVICES.add("com.alipay.sofa.rpc.filter.Filter");
        OTHER_SERVICES.add("com.taobao.hsf.invocation.filter.RPCFilter");
        OTHER_SERVICES.add("com.weibo.api.motan.filter.Filter");
    }


    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        // Register the services to reflection hints in 'META-INF/services', only the services required by seata.
        Predicate<Resource> predicate = this::isSeataServicesResource;
        AotUtils.registerServices(hints.reflection(), "classpath*:" + SERVICES_DIRECTORY + "*", predicate, AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE);
        AotUtils.registerServices(hints.reflection(), "classpath*:" + SEATA_DIRECTORY + "*", predicate, AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE);
    }


    private boolean isSeataServicesResource(Resource resource) {
        if (resource.getFilename() == null) {
            return false;
        }

        if (resource.getFilename().startsWith("io.seata.")) {
            return true;
        }

        return OTHER_SERVICES.contains(resource.getFilename());
    }

}
