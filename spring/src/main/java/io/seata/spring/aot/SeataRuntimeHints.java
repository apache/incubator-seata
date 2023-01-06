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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import io.seata.common.util.ReflectionUtil;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.spring.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import static io.seata.spring.aot.AotUtils.ALL_MEMBER_CATEGORIES;
import static io.seata.spring.aot.AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE;
import static io.seata.spring.aot.AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE_AND_INVOKE;

/**
 * The seata runtime hints registrar
 *
 * @author wang.liang
 */
class SeataRuntimeHints implements RuntimeHintsRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataRuntimeHints.class);

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

        // Register the services in 'META-INF/services'
        Resource[] resources = ResourceUtil.getResources("classpath*:META-INF/services/*");
        for (Resource resource : resources) {
            if (!this.isSeataServicesResource(resource)) {
                // Register only the services required by seata.
                continue;
            }

            try (InputStreamReader isr = new InputStreamReader(resource.getInputStream());
                 BufferedReader br = new BufferedReader(isr)) {
                br.lines().forEach(className -> {
                    AotUtils.registerReflectionType(reflectionHints,
                            MEMBER_CATEGORIES_FOR_INSTANTIATE,
                            className);
                });
            } catch (IOException e) {
                LOGGER.error("Register services '{}' fail: {}", resource.getFilename(), e.getMessage(), e);
            }
        }

        // Register the seata classes
        AotUtils.registerReflectionType(reflectionHints,
                MEMBER_CATEGORIES_FOR_INSTANTIATE,
                "io.seata.sqlparser.druid.DruidDbTypeParserImpl",
                "io.seata.sqlparser.druid.DruidSQLRecognizerFactoryImpl",
                "io.seata.sqlparser.antlr.mysql.AntlrMySQLRecognizerFactory",
                "io.seata.serializer.protobuf.ProtobufSerializer"
        );
        AotUtils.registerReflectionType(reflectionHints,
                ALL_MEMBER_CATEGORIES,
                BranchUndoLog.class,
                SQLUndoLog.class,
                TableRecords.class,
                TableRecords.EmptyTableRecords.class,
                io.seata.rm.datasource.sql.struct.Row.class,
                io.seata.rm.datasource.sql.struct.Field.class
        );

        // Register the MySQL classes for AT mode, see the class 'io.seata.rm.datasource.sql.struct.cache.AbstractTableMetaCache'
        Set<Class<?>> classes = ReflectionUtil.getClassesByPackage("com.github.benmanes.caffeine.cache");
        if (classes.size() > 0) {
            for (Class<?> clazz : classes) {
                String simpleClassName = clazz.getSimpleName();
                if (simpleClassName.length() > 0 && simpleClassName.toUpperCase().equals(simpleClassName)) {
                    AotUtils.registerReflectionType(reflectionHints,
                            MEMBER_CATEGORIES_FOR_INSTANTIATE,
                            clazz);
                }
            }
        }

        // Register the MySQL classes for XA mode, see the class 'com.alibaba.druid.util.MySqlUtils'
        AotUtils.registerReflectionType(reflectionHints,
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

//        hints.resources().registerPattern("lib/sqlparser/druid.jar");
//        hints.resources().registerPattern("META-INF/services/io.seata.*");
//        hints.resources().registerPattern("META-INF/seata/io.seata.*");
//        for (String servicesFileName : OTHER_SERVICES) {
//            hints.resources().registerPattern("META-INF/services/" + servicesFileName);
//            hints.resources().registerPattern("META-INF/seata/" + servicesFileName);
//        }
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
