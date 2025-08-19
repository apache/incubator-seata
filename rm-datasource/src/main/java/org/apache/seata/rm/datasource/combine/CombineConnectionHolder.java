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
package org.apache.seata.rm.datasource.combine;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.rm.datasource.xa.ConnectionProxyXA;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CombineConnectionHolder {
    private static final ThreadLocal<Map<String, Map<Object, ConnectionProxyXA>>> CONNECTION_HOLDER =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    public static ConnectionProxyXA get(DataSource dataSource) {
        Map<Object, ConnectionProxyXA> connMap = CONNECTION_HOLDER.get().get(RootContext.getXID());
        if (connMap != null) {
            return connMap.get(dataSource);
        }
        return null;
    }

    public static Collection<ConnectionProxyXA> getDsConn() {
        Map<Object, ConnectionProxyXA> connectionMap = CONNECTION_HOLDER.get().get(RootContext.getXID());
        return connectionMap != null ? connectionMap.values() : Collections.emptyList();
    }

    public static void putConnection(DataSource dataSource, ConnectionProxyXA connection) throws SQLException {
        Map<String, Map<Object, ConnectionProxyXA>> concurrentHashMap = CONNECTION_HOLDER.get();
        String xid = RootContext.getXID();
        Map<Object, ConnectionProxyXA> connectionProxyMap =
                concurrentHashMap.computeIfAbsent(xid, k -> new ConcurrentHashMap<>());

        if (connectionProxyMap.putIfAbsent(dataSource, connection) == null) {
            connection.setAutoCommit(false);
            connection.setCombine(true);
        }
    }

    public static void clear() {
        CONNECTION_HOLDER.get().remove(RootContext.getXID());
    }
}
