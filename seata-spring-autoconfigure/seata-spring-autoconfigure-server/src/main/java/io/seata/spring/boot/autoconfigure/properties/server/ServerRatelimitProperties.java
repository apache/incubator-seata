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
package io.seata.spring.boot.autoconfigure.properties.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RATELIMIT_PREFIX;

@Component
@ConfigurationProperties(prefix = SERVER_RATELIMIT_PREFIX)
public class ServerRatelimitProperties {
    private boolean enableServerRatelimit;

    private int requestsPerSecond;

    private int burst;

    private boolean delay;

    public boolean getEnableServerRatelimit() {
        return enableServerRatelimit;
    }

    public ServerRatelimitProperties setEnableServerRatelimit(boolean enableServerRatelimit) {
        this.enableServerRatelimit = enableServerRatelimit;
        return this;
    }

    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public ServerRatelimitProperties setRequestsPerSecond(int requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
        return this;
    }

    public int getBurst() {
        return burst;
    }

    public ServerRatelimitProperties setBurst(int burst) {
        this.burst = burst;
        return this;
    }

    public boolean getDelay() {
        return delay;
    }

    public ServerRatelimitProperties setDelay(boolean delay) {
        this.delay = delay;
        return this;
    }
}
