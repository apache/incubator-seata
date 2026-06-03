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
package org.apache.seata.mcp.entity.dto;

import org.springaicommunity.mcp.annotation.McpToolParam;

public class MysqlDataSourceRegisterRequest {

    @McpToolParam(
            description = "Unique data source name, used to build resourceId business-ds://{name}",
            required = true)
    private String name;

    @McpToolParam(
            description =
                    "MySQL JDBC URL that must include a database name, for example jdbc:mysql://host:3306/student",
            required = true)
    private String url;

    @McpToolParam(description = "MySQL username. Use a read-only database account for query tools", required = true)
    private String username;

    @McpToolParam(
            description =
                    "Server-side secret reference used to resolve the MySQL password, for example STUDENT_DB_PASSWORD",
            required = true)
    private String passwordSecretRef;

    @McpToolParam(
            description = "Connection pool type. Supported values depend on the local provider, default is druid",
            required = false)
    private String datasource = "druid";

    @McpToolParam(description = "Minimum connection pool size, default is 10", required = false)
    private int minConn = 10;

    @McpToolParam(description = "Maximum connection pool size, default is 100", required = false)
    private int maxConn = 100;

    @McpToolParam(
            description = "Maximum time in milliseconds to wait for a connection, default is 5000",
            required = false)
    private Long maxWait = 5000L;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordSecretRef() {
        return passwordSecretRef;
    }

    public void setPasswordSecretRef(String passwordSecretRef) {
        this.passwordSecretRef = passwordSecretRef;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public int getMinConn() {
        return minConn;
    }

    public void setMinConn(int minConn) {
        this.minConn = minConn;
    }

    public int getMaxConn() {
        return maxConn;
    }

    public void setMaxConn(int maxConn) {
        this.maxConn = maxConn;
    }

    public Long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(Long maxWait) {
        this.maxWait = maxWait;
    }
}
