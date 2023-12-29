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
package io.seata.mockserver.controller;

import io.seata.mockserver.ExpectTransactionResult;
import io.seata.mockserver.MockCoordinator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ?
 *
 * @author minghua.xie
 * @date 2023/12/29
 **/
@RequestMapping("/help")
public class MockHelpController {

    static String OK = "ok";

    @GetMapping("/health")
    public String health() {
        return OK;
    }

    @PostMapping("/expect/status")
    public String expectTransactionResult(@RequestParam String xid, @RequestParam int code) {
        MockCoordinator.getInstance().setExpectedResult(xid, ExpectTransactionResult.covert(code));
        return OK;
    }

    @PostMapping("/expect/retry")
    public String expectTransactionRetry(@RequestParam String xid, @RequestParam int times) {
        MockCoordinator.getInstance().setExpectedRetry(xid, times);
        return OK;
    }


}
