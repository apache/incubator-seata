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

import java.util.Collections;
import java.util.List;

public class MysqlDataSourceRegisterRequest {

    private String name;
    private String url;
    private String username;
    private String passwordSecretRef;
    private String password;
    private String datasource = "druid";
    private int minConn = 10;
    private int maxConn = 100;
    private Long maxWait = 5000L;
    private List<String> allowedSchemas = Collections.emptyList();

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public List<String> getAllowedSchemas() {
        return allowedSchemas;
    }

    public void setAllowedSchemas(List<String> allowedSchemas) {
        this.allowedSchemas = allowedSchemas == null ? Collections.emptyList() : allowedSchemas;
    }
}
