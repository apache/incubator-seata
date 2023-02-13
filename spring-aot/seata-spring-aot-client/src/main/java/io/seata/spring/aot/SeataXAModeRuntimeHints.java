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

import java.sql.Connection;
import java.sql.Driver;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

/**
 * The seata XA mode runtime hints registrar
 *
 * @author wang.liang
 */
class SeataXAModeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        // Register the MySQL classes
        this.registerMySQLClasses(hints);

        // Register the Oracle classes
        this.registerOracleClasses(hints);
    }

    /**
     * Register the MySQL classes for XA mode.
     *
     * @param hints the runtime hints
     * @see com.alibaba.druid.util.MySqlUtils
     */
    private void registerMySQLClasses(RuntimeHints hints) {
        AotUtils.registerTypes(hints.reflection(),
                AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE_AND_INVOKE,
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

    /**
     * Register the Oracle classes for XA mode.
     *
     * @param hints the runtime hints
     * @see io.seata.rm.datasource.util.XAUtils#createXAConnection(Connection, Driver, String)
     */
    private void registerOracleClasses(RuntimeHints hints) {
        AotUtils.registerTypes(hints.reflection(),
                AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE,
                "oracle.jdbc.driver.T4CXAConnection",
                "oracle.jdbc.xa.client.OracleXAConnection"
        );
    }
}
