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
package io.seata.server.env;

import io.seata.common.util.NumberUtils;

import static io.seata.common.DefaultValues.SERVER_DEFAULT_PORT;

/**
 * @author wang.liang
 */
public class PortHelper {

    public static int getPort(String[] args) {
        if (ContainerHelper.isRunningInContainer()) {
            return ContainerHelper.getPort();
        } else if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length; ++i) {
                if ("-p".equalsIgnoreCase(args[i]) && i < args.length - 1) {
                    return NumberUtils.toInt(args[i + 1], SERVER_DEFAULT_PORT);
                }
            }
        }

        return SERVER_DEFAULT_PORT;
    }

}
