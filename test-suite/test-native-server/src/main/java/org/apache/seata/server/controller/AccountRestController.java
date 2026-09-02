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
package org.apache.seata.server.controller;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.server.dto.AccountMoneyRequest;
import org.apache.seata.server.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountRestController.class);

    private AccountService accountService;

    @Autowired
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Modify user balance
     *
     * @param request request parameters
     * @param keyXid  distributed transaction ID
     */
    @PostMapping("/money")
    public Map<String, Object> money(
            @RequestBody AccountMoneyRequest request,
            @RequestHeader(value = RootContext.KEY_XID, required = false) String keyXid) {
        LOGGER.info("Distributed transaction {}: {}", RootContext.KEY_XID, keyXid);

        accountService.money(request);
        return Map.of("code", 200);
    }

    @GetMapping("/money/{userId}")
    public long money(@PathVariable String userId) {
        return accountService.getMoney(userId);
    }
}
