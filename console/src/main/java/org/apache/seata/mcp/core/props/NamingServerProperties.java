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
package org.apache.seata.mcp.core.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ConfigurationProperties(prefix = "console.namingserver")
@Component
public class NamingServerProperties {

    /**
     * http, https
     */
    private String protocol = "http";

    private List<String> addr = Collections.singletonList("127.0.0.1:8081");

    public String getNamingServerUrl() {
        if (addr == null || addr.isEmpty()) {
            throw new IllegalStateException("No naming servers addr configured");
        }
        int index = ThreadLocalRandom.current().nextInt(addr.size());
        return protocol + "://" + addr.get(index);
    }

    public void setAddr(List<String> addr) {
        this.addr = addr;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
