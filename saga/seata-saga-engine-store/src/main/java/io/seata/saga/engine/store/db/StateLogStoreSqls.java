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
 * State log store sqls
 *
 * @author lorne.cl
 */
public class StateLogStoreSqls {

    /**
     * machine instance
     **/
    private static final String STATE_MACHINE_INSTANCE_FIELDS
        = "id, machine_id, tenant_id, parent_id, business_key, gmt_started, gmt_end, status, compensation_status, "
        + "is_running, gmt_updated, start_params, end_params, excep";

    private static final String STATE_MACHINE_INSTANCE_FIELDS_WITHOUT_PARAMS
        = "id, machine_id, tenant_id, parent_id, business_key, gmt_started, gmt_end, status, compensation_status, "
        + "is_running, gmt_updated";

    private static final String RECORD_STATE_MACHINE_STARTED_SQL = "INSERT INTO ${TABLE_PREFIX}state_machine_inst\n"
        + "(id, machine_id, tenant_id, parent_id, gmt_started, business_key, start_params, is_running, status, "
        + "gmt_updated)\n"
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)";

    private static final String RECORD_STATE_MACHINE_FINISHED_SQL
        = "UPDATE ${TABLE_PREFIX}state_machine_inst SET gmt_end = ?, excep = ?, end_params = ?,status = ?, "
        + "compensation_status = ?, is_running = ?, gmt_updated = current_timestamp WHERE id = ?";

    private static final String UPDATE_STATE_MACHINE_RUNNING_STATUS_SQL =
        "UPDATE ${TABLE_PREFIX}state_machine_inst SET\n"
            + "is_running = ?, gmt_updated = current_timestamp where id = ?";

    private static final String GET_STATE_MACHINE_INSTANCE_BY_ID_SQL = "SELECT " + STATE_MACHINE_INSTANCE_FIELDS
        + " FROM ${TABLE_PREFIX}state_machine_inst WHERE id = ?";

    private static final String GET_STATE_MACHINE_INSTANCE_BY_BUSINESS_KEY_SQL = "SELECT "
        + STATE_MACHINE_INSTANCE_FIELDS
        + " FROM ${TABLE_PREFIX}state_machine_inst WHERE business_key = ? AND tenant_id = ?";

    private static final String QUERY_STATE_MACHINE_INSTANCES_BY_PARENT_ID_SQL = "SELECT "
        + STATE_MACHINE_INSTANCE_FIELDS_WITHOUT_PARAMS
        + " FROM ${TABLE_PREFIX}state_machine_inst WHERE parent_id = ? ORDER BY gmt_started DESC";

    /**
     * state instance
     **/
    private static final String STATE_INSTANCE_FIELDS
        = "id, machine_inst_id, name, type, business_key, gmt_started, service_name, service_method, service_type, "
        + "is_for_update, status, input_params, output_params, excep, gmt_end, state_id_compensated_for, "
        + "state_id_retried_for";

    private static final String RECORD_STATE_STARTED_SQL =
        "INSERT INTO ${TABLE_PREFIX}state_inst (id, machine_inst_id, name, type,"
            + " gmt_started, service_name, service_method, service_type, is_for_update, input_params, status, "
            + "business_key, "
            + "state_id_compensated_for, state_id_retried_for)\n" + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String RECORD_STATE_FINISHED_SQL
        = "UPDATE ${TABLE_PREFIX}state_inst SET gmt_end = ?, excep = ?, status = ?, output_params = ? WHERE id = ? "
        + "AND machine_inst_id = ?";

    private static final String UPDATE_STATE_EXECUTION_STATUS_SQL
        = "UPDATE ${TABLE_PREFIX}state_inst SET status = ? WHERE machine_inst_id = ? AND id = ?";

    private static final String QUERY_STATE_INSTANCES_BY_MACHINE_INSTANCE_ID_SQL = "SELECT " + STATE_INSTANCE_FIELDS
        + " FROM ${TABLE_PREFIX}state_inst WHERE machine_inst_id = ? ORDER BY gmt_started, ID ASC";

    private static final String GET_STATE_INSTANCE_BY_ID_AND_MACHINE_INSTANCE_ID_SQL = "SELECT " + STATE_INSTANCE_FIELDS
        + " FROM ${TABLE_PREFIX}state_inst WHERE machine_inst_id = ? AND id = ?";

    private static final String TABLE_PREFIX_REGEX = "\\$\\{TABLE_PREFIX}";

    private String tablePrefix;

    /**
     * machine instance
     **/
    private String recordStateMachineStartedSql;

    private String recordStateMachineFinishedSql;

    private String updateStateMachineRunningStatusSql;

    private String getStateMachineInstanceByIdSql;

    private String getStateMachineInstanceByBusinessKeySql;

    private String queryStateMachineInstancesByParentIdSql;

    /**
     * state instance
     **/
    private String recordStateStartedSql;

    private String recordStateFinishedSql;

    private String updateStateExecutionStatusSql;

    private String queryStateInstancesByMachineInstanceIdSql;

    private String getStateInstanceByIdAndMachineInstanceIdSql;

    public StateLogStoreSqls(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        init();
    }

    private void init() {
        recordStateMachineStartedSql = RECORD_STATE_MACHINE_STARTED_SQL.replaceAll(TABLE_PREFIX_REGEX, tablePrefix);
        recordStateMachineFinishedSql = RECORD_STATE_MACHINE_FINISHED_SQL.replaceAll(TABLE_PREFIX_REGEX, tablePrefix);
        updateStateMachineRunningStatusSql = UPDATE_STATE_MACHINE_RUNNING_STATUS_SQL.replaceAll(TABLE_PREFIX_REGEX,
            tablePrefix);
        getStateMachineInstanceByIdSql = GET_STATE_MACHINE_INSTANCE_BY_ID_SQL.replaceAll(TABLE_PREFIX_REGEX,
            tablePrefix);
        getStateMachineInstanceByBusinessKeySql = GET_STATE_MACHINE_INSTANCE_BY_BUSINESS_KEY_SQL.replaceAll(
            TABLE_PREFIX_REGEX, tablePrefix);
        queryStateMachineInstancesByParentIdSql = QUERY_STATE_MACHINE_INSTANCES_BY_PARENT_ID_SQL.replaceAll(
            TABLE_PREFIX_REGEX, tablePrefix);

        recordStateStartedSql = RECORD_STATE_STARTED_SQL.replaceAll(TABLE_PREFIX_REGEX, tablePrefix);
        recordStateFinishedSql = RECORD_STATE_FINISHED_SQL.replaceAll(TABLE_PREFIX_REGEX, tablePrefix);
        updateStateExecutionStatusSql = UPDATE_STATE_EXECUTION_STATUS_SQL.replaceAll(TABLE_PREFIX_REGEX, tablePrefix);
        queryStateInstancesByMachineInstanceIdSql = QUERY_STATE_INSTANCES_BY_MACHINE_INSTANCE_ID_SQL.replaceAll(
            TABLE_PREFIX_REGEX, tablePrefix);
        getStateInstanceByIdAndMachineInstanceIdSql = GET_STATE_INSTANCE_BY_ID_AND_MACHINE_INSTANCE_ID_SQL.replaceAll(
            TABLE_PREFIX_REGEX, tablePrefix);
    }

    public String getRecordStateMachineStartedSql(String dbType) {
        return recordStateMachineStartedSql;
    }

    public String getRecordStateMachineFinishedSql(String dbType) {
        return recordStateMachineFinishedSql;
    }

    public String getUpdateStateMachineRunningStatusSql(String dbType) {
        return updateStateMachineRunningStatusSql;
    }

    public String getGetStateMachineInstanceByIdSql(String dbType) {
        return getStateMachineInstanceByIdSql;
    }

    public String getGetStateMachineInstanceByBusinessKeySql(String dbType) {
        return getStateMachineInstanceByBusinessKeySql;
    }

    public String getQueryStateMachineInstancesByParentIdSql(String dbType) {
        return queryStateMachineInstancesByParentIdSql;
    }

    public String getRecordStateStartedSql(String dbType) {
        return recordStateStartedSql;
    }

    public String getRecordStateFinishedSql(String dbType) {
        return recordStateFinishedSql;
    }

    public String getUpdateStateExecutionStatusSql(String dbType) {
        return updateStateExecutionStatusSql;
    }

    public String getQueryStateInstancesByMachineInstanceIdSql(String dbType) {
        return queryStateInstancesByMachineInstanceIdSql;
    }

    public String getGetStateInstanceByIdAndMachineInstanceIdSql(String dbType) {
        return getStateInstanceByIdAndMachineInstanceIdSql;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }
}