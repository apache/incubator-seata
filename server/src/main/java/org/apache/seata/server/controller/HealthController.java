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

import org.apache.seata.core.rpc.netty.http.HttpController;
import org.apache.seata.server.ServerRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@Component
public class HealthController implements HttpController {

    private static final String OK = "ok";
    private static final String NOT_OK = "not_ok";

    @Autowired
    private ServerRunner serverRunner;

    @Override
    public Set<String> getPath() {
        return new HashSet<String>() {{
            add("/health");
        }};
    }

    @Override
    public String handle(String path, Map<String, List<String>> paramMap) {
        return serverRunner.started() ? OK : NOT_OK;
    }
}
