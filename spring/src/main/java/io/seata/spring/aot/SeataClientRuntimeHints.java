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
import javax.sql.DataSource;

import io.seata.common.util.ReflectionUtil;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.SQLUndoLog;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import static io.seata.common.DefaultValues.DRUID_LOCATION;
import static io.seata.spring.aot.AotUtils.ALL_MEMBER_CATEGORIES;
import static io.seata.spring.aot.AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE;
import static io.seata.spring.aot.AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE_AND_INVOKE;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;

/**
 * The seata-client runtime hints registrar
 *
 * @author wang.liang
 */
class SeataClientRuntimeHints implements RuntimeHintsRegistrar {

    private static final Set<String> OTHER_SERVICES = new HashSet<>();

    static {
        OTHER_SERVICES.add("com.alibaba.dubbo.rpc.Filter");
        OTHER_SERVICES.add("com.alipay.sofa.rpc.filter.Filter");
        OTHER_SERVICES.add("com.taobao.hsf.invocation.filter.RPCFilter");
        OTHER_SERVICES.add("com.weibo.api.motan.filter.Filter");
    }


    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        ReflectionHints reflectionHints = hints.reflection();

        // Register '/META-INF/services/*'
        this.registerServices(hints);

        // Register for AT mode
        this.registerHintsForATMode(hints);

        // Register for XA mode
        this.registerHintsForXAMode(hints);

        // Register the seata classes
        AotUtils.registerTypes(reflectionHints,
                MEMBER_CATEGORIES_FOR_INSTANTIATE,
                "io.seata.sqlparser.druid.DruidDbTypeParserImpl", // see DruidDelegatingDbTypeParser
                "io.seata.sqlparser.druid.DruidSQLRecognizerFactoryImpl", // see DruidDelegatingSQLRecognizerFactory
                "io.seata.sqlparser.antlr.mysql.AntlrMySQLRecognizerFactory", // see AntlrDelegatingSQLRecognizerFactory
                "io.seata.serializer.protobuf.ProtobufSerializer" // see SerializerServiceLoader
        );
    }


    private void registerServices(RuntimeHints hints) {
        // Register the services to reflection hints in 'META-INF/services', only the services required by seata.
        Predicate<Resource> predicate = this::isSeataServicesResource;
        AotUtils.registerServices(hints.reflection(), predicate, MEMBER_CATEGORIES_FOR_INSTANTIATE);

        // Register the service files to resources hints.
        ResourceHints resourceHints = hints.resources();
        resourceHints.registerPattern("META-INF/services/io.seata.*");
        resourceHints.registerPattern("META-INF/seata/io.seata.*");
        for (String serviceFileName : OTHER_SERVICES) {
            resourceHints.registerPattern("META-INF/services/" + serviceFileName);
            resourceHints.registerPattern("META-INF/seata/" + serviceFileName);
        }
    }

    private void registerHintsForATMode(RuntimeHints hints) {
        ReflectionHints reflectionHints = hints.reflection();

        // Register implementation classes of 'com.github.benmanes.caffeine.cache.NodeFactory'.
        // See the class  'io.seata.rm.datasource.sql.struct.cache.AbstractTableMetaCache'
        // See the method 'com.github.benmanes.caffeine.cache.NodeFactory.newFactory(Caffeine<K, V> builder, boolean isAsync)'
        String caffeineCachePackage = "com.github.benmanes.caffeine.cache";
        Set<Class<?>> classes = ReflectionUtil.getClassesByPackage(caffeineCachePackage);
        if (classes.size() > 0) {
            for (Class<?> clazz : classes) {
                String simpleClassName = clazz.getSimpleName();
                if (simpleClassName.length() > 0 && simpleClassName.toUpperCase().equals(simpleClassName)) {
                    AotUtils.registerType(reflectionHints, clazz, MEMBER_CATEGORIES_FOR_INSTANTIATE);
                }
            }
        }

        // Register DataSource for 'io.seata.spring.annotation.datasource.SeataAutoDataSourceProxyAdvice'
        AotUtils.registerType(reflectionHints, DataSource.class, INVOKE_DECLARED_METHODS);

        // Register the beans for serialize
        AotUtils.registerTypes(reflectionHints,
                ALL_MEMBER_CATEGORIES,
                BranchUndoLog.class,
                SQLUndoLog.class,
                TableRecords.class,
                TableRecords.EmptyTableRecords.class,
                io.seata.rm.datasource.sql.struct.Row.class,
                io.seata.rm.datasource.sql.struct.Field.class
        );

        // Register 'druid.jar' for 'io.seata.sqlparser.druid.DefaultDruidLoader'
        hints.resources().registerPattern(DRUID_LOCATION);
    }

    private void registerHintsForXAMode(RuntimeHints hints) {
        // Register the MySQL classes for XA mode.
        // See the class 'com.alibaba.druid.util.MySqlUtils'
        AotUtils.registerTypes(hints.reflection(),
                MEMBER_CATEGORIES_FOR_INSTANTIATE_AND_INVOKE,
                "com.mysql.cj.api.conf.PropertySet",
                "com.mysql.cj.api.conf.ReadableProperty",
                "com.mysql.cj.api.jdbc.JdbcConnection",
                "com.mysql.cj.conf.PropertySet",
                "com.mysql.cj.conf.ReadableProperty",
                "com.mysql.cj.conf.RuntimeProperty",
                "com.mysql.cj.jdbc.JdbcConnection",
                "com.mysql.cj.jdbc.MysqlXAConnection",
                "com.mysql.cj.jdbc.SuspendableXAConnection"
        );
    }


    private boolean isSeataServicesResource(Resource resource) {
        if (resource.getFilename() == null) {
            return false;
        }

        if (resource.getFilename().startsWith("io.seata")) {
            return true;
        }

        return OTHER_SERVICES.contains(resource.getFilename());
    }

}
