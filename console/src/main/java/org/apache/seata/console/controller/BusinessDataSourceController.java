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
package org.apache.seata.console.controller;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.console.security.DataSourcePasswordCipher;
import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceInfo;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceTestResult;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businessDataSources")
public class BusinessDataSourceController {

    private final BusinessDataSourceService dataSourceService;

    private final DataSourcePasswordCipher passwordCipher;

    public BusinessDataSourceController(
            BusinessDataSourceService dataSourceService, DataSourcePasswordCipher passwordCipher) {
        this.dataSourceService = dataSourceService;
        this.passwordCipher = passwordCipher;
    }

    @GetMapping
    public SingleResult<List<MysqlDataSourceInfo>> listDataSources() {
        return SingleResult.success(dataSourceService.getMysqlDataSources());
    }

    @PostMapping
    public SingleResult<String> registerDataSource(@RequestBody MysqlDataSourceRegisterRequest request) {
        try {
            decryptPassword(request);
            return SingleResult.success(dataSourceService.registerMysqlDataSource(request));
        } catch (Exception e) {
            return SingleResult.failure(e.getMessage());
        }
    }

    @PostMapping("/test")
    public SingleResult<MysqlDataSourceTestResult> testDataSource(@RequestBody MysqlDataSourceRegisterRequest request) {
        try {
            decryptPassword(request);
            return SingleResult.success(dataSourceService.testMysqlDataSource(request));
        } catch (Exception e) {
            MysqlDataSourceTestResult result = new MysqlDataSourceTestResult();
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return SingleResult.success(result);
        }
    }

    @DeleteMapping("/{name}")
    public SingleResult<String> unregisterDataSource(@PathVariable String name) {
        try {
            return SingleResult.success(dataSourceService.unregisterMysqlDataSource(name));
        } catch (Exception e) {
            return SingleResult.failure(e.getMessage());
        }
    }

    private void decryptPassword(MysqlDataSourceRegisterRequest request) {
        if (request == null || StringUtils.isBlank(request.getEncryptedPassword())) {
            return;
        }
        request.setPassword(passwordCipher.decrypt(request.getEncryptedPassword()));
        request.setEncryptedPassword("");
    }
}
