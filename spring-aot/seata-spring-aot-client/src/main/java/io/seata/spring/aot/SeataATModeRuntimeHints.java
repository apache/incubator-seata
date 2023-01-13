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

import javax.sql.DataSource;

import io.seata.common.DefaultValues;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;

/**
 * The seata AT mode runtime hints registrar
 *
 * @author wang.liang
 */
class SeataATModeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        ReflectionHints reflectionHints = hints.reflection();

        // Register DataSource for 'io.seata.spring.annotation.datasource.SeataAutoDataSourceProxyAdvice#invoke'
        AotUtils.registerType(reflectionHints, DataSource.class, INVOKE_DECLARED_METHODS);

        // Register 'druid.jar' for 'io.seata.sqlparser.druid.DefaultDruidLoader#DRUID_LOADER'
        hints.resources().registerPattern(DefaultValues.DRUID_LOCATION);

        // Register following classes for SQL parser
        AotUtils.registerTypes(reflectionHints,
                AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE,
                "io.seata.sqlparser.druid.DruidDbTypeParserImpl", // See DruidDelegatingDbTypeParser
                "io.seata.sqlparser.druid.DruidSQLRecognizerFactoryImpl", // See DruidDelegatingSQLRecognizerFactory
                "io.seata.sqlparser.antlr.mysql.AntlrMySQLRecognizerFactory" // See AntlrDelegatingSQLRecognizerFactory
        );

        // Register the classes for serializer
        AotUtils.registerTypesForSerialize(reflectionHints,
                "io.seata.rm.datasource.undo.BranchUndoLog",
                "io.seata.rm.datasource.undo.SQLUndoLog",
                "io.seata.rm.datasource.sql.struct.TableRecords",
                "io.seata.rm.datasource.sql.struct.TableRecords$EmptyTableRecords",
                "io.seata.rm.datasource.sql.struct.Row",
                "io.seata.rm.datasource.sql.struct.Field"
        );

        /*
         * Register implementation classes of 'com.github.benmanes.caffeine.cache.NodeFactory'.
         * Only register the classes used.
         *
         * See io.seata.rm.datasource.sql.struct.cache.AbstractTableMetaCache
         * See com.github.benmanes.caffeine.cache.NodeFactory#newFactory(Caffeine<K, V> builder, boolean isAsync)
         */
        AotUtils.registerTypes(reflectionHints,
                AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE,
                "com.github.benmanes.caffeine.cache.PDWMS",
                "com.github.benmanes.caffeine.cache.SIMSW"
        );
    }

}
