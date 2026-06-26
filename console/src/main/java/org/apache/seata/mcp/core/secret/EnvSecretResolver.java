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
package org.apache.seata.mcp.core.secret;

import org.apache.seata.common.util.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvSecretResolver implements SecretResolver {

    private final Environment environment;

    public EnvSecretResolver(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String resolve(String secretRef) {
        if (!StringUtils.hasText(secretRef)) {
            throw new IllegalArgumentException("passwordSecretRef cannot be empty");
        }
        String secret = environment.getProperty(secretRef);
        if (!StringUtils.hasText(secret)) {
            secret = System.getenv(secretRef);
        }
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("Unable to resolve password secret: " + secretRef);
        }
        return secret;
    }
}
