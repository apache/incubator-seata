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
package io.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_SHUTDOWN_TIMEOUT_SEC;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SHUTDOWN_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = SHUTDOWN_PREFIX)
public class ShutdownProperties {
    /**
     * when destroy server, wait seconds
     */
    private long wait = DEFAULT_SHUTDOWN_TIMEOUT_SEC;

    public long getWait() {
        return wait;
    }

    public ShutdownProperties setWait(long wait) {
        this.wait = wait;
        return this;
    }
}
