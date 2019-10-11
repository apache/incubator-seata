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
package io.seata.saga.engine.store.db;

/**
 * StateLang store sqls
 *
 * @author lorne.cl
 */
public class StateLangStoreSqls {

    private static final String STATE_MACHINE_FIELDS = "id, tenant_id, app_name, name, status, gmt_create, ver, type, content, recover_strategy, comment_";

    private static final String GET_STATE_MACHINE_BY_ID_SQL = "SELECT " + STATE_MACHINE_FIELDS + " FROM ${TABLE_PREFIX}state_machine_def WHERE id = ?";

    private static final String QUERY_STATE_MACHINES_BY_NAME_AND_TENANT_SQL = "SELECT " + STATE_MACHINE_FIELDS + " FROM ${TABLE_PREFIX}state_machine_def WHERE name = ? AND tenant_id = ? ORDER BY gmt_create DESC";

    private static final String INSERT_STATE_MACHINE_SQL = "INSERT INTO ${TABLE_PREFIX}state_machine_def (" + STATE_MACHINE_FIELDS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String TABLE_PREFIX_REGEX = "\\$\\{TABLE_PREFIX}";

    private String tablePrefix;

    private String getGetStateMachineByIdSql;
    private String queryStateMachinesByNameAndTenantSql;
    private String insertStateMachineSql;

    public StateLangStoreSqls(String tablePrefix){
        this.tablePrefix = tablePrefix;
        init();
    }

    private void init(){
        getGetStateMachineByIdSql = GET_STATE_MACHINE_BY_ID_SQL.replaceAll(TABLE_PREFIX_REGEX, tablePrefix);
        queryStateMachinesByNameAndTenantSql = QUERY_STATE_MACHINES_BY_NAME_AND_TENANT_SQL.replaceAll(TABLE_PREFIX_REGEX, tablePrefix);
        insertStateMachineSql = INSERT_STATE_MACHINE_SQL.replaceAll(TABLE_PREFIX_REGEX, tablePrefix);
    }

    public String getGetStateMachineByIdSql(String dbType){
        return getGetStateMachineByIdSql;
    }

    public String getQueryStateMachinesByNameAndTenantSql(String dbType){
        return queryStateMachinesByNameAndTenantSql;
    }

    public String getInsertStateMachineSql(String dbType){
        return insertStateMachineSql;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }
}