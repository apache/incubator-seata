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
package io.seata.config.springcloud;

import io.seata.common.loader.LoadLevel;
import io.seata.config.Configuration;
import io.seata.config.source.ConfigSourceProvider;

import static io.seata.config.source.ConfigSourceOrdered.CONFIG_CENTER_SOURCE_ORDER;

@LoadLevel(name = "SpringCloudConfig", order = CONFIG_CENTER_SOURCE_ORDER)
public class SpringCloudConfigSourceProvider implements ConfigSourceProvider {
    @Override
    public void provide(Configuration configuration) {
        configuration.addSource(SpringCloudConfigSource.getInstance());
    }
}
