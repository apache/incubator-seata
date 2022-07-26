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
package io.seata.server.console.controller;

import io.seata.console.result.PageResult;
import io.seata.console.result.Result;
import io.seata.server.console.param.ClientOfflineParam;
import io.seata.server.console.param.ClientQueryParam;
import io.seata.server.console.service.ClientService;
import io.seata.server.console.vo.ClientVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author TheR1sing3un
 * @date 2022/7/26 22:12
 * @description Web controller about client(keep-alive-connection)
 */

@RestController
@RequestMapping("/api/v1/console/client")
public class ClientController {

    @Resource
    ClientService clientService;

    @GetMapping("/query")
    public PageResult<ClientVO> query(@ModelAttribute ClientQueryParam param) {
        return clientService.query(param);
    }

    @DeleteMapping("/offline")
    public Result offline(@ModelAttribute ClientOfflineParam param) {
        return clientService.offline(param);
    }

}
