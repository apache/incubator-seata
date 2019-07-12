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
package io.seata.discovery.registry.eureka;

import io.seata.common.loader.LoadLevel;
import io.seata.discovery.registry.RegistryService;
import io.seata.discovery.registry.RegistryProvider;

/**
 * @author xingfudeshi@gmail.com
 * @date 2019/04/12
 */
@LoadLevel(name = "Eureka", order = 1)
public class EurekaRegistryProvider implements RegistryProvider {
    @Override
    public RegistryService provide() {
        return EurekaRegistryServiceImpl.getInstance();
    }
}
