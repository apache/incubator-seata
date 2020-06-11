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
package io.seata.server.logging.logback;

import java.lang.management.ManagementFactory;

/**
 * An application process ID.
 *
 * @author wang.liang
 */
public class ApplicationPid {

    private final String pid;

    public ApplicationPid() {
        this.pid = getPid();
    }

    protected ApplicationPid(String pid) {
        this.pid = pid;
    }

    private String getPid() {
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            return jvmName.split("@")[0];
        }
        catch (Throwable ex) {
            return null;
        }
    }

    @Override
    public String toString() {
        return (this.pid != null) ? this.pid : "???";
    }
}
