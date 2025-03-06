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
package org.apache.seata.server.instance;

import java.util.UUID;

import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.server.store.StoreConfig;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import static org.apache.seata.common.ConfigurationKeys.META_PREFIX;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

public class GeneralInstanceStrategy extends AbstractSeataInstanceStrategy {

    @Override
    public Instance serverInstanceInit() {

        ConfigurableEnvironment environment =
            (ConfigurableEnvironment)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);

        // load node properties
        Instance instance = Instance.getInstance();
        // load namespace
        String namespace = registryNamingServerProperties.getNamespace();
        instance.setNamespace(namespace);
        // load cluster name
        String clusterName = registryNamingServerProperties.getCluster();
        instance.setClusterName(clusterName);

        // load cluster type
        String clusterType = String.valueOf(StoreConfig.getSessionMode());
        instance.addMetadata("cluster-type", "raft".equals(clusterType) ? clusterType : "default");

        // load unit name
        instance.setUnit(String.valueOf(UUID.randomUUID()));

        instance.setTerm(System.currentTimeMillis());

        // load node Endpoint
        instance.setControl(new Node.Endpoint(XID.getIpAddress(), serverProperties.getPort(), "http"));

        // load metadata
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>)propertySource;
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    if (propertyName.startsWith(META_PREFIX)) {
                        instance.addMetadata(propertyName.substring(META_PREFIX.length()),
                            enumerablePropertySource.getProperty(propertyName));
                    }
                }
            }
        }
        return instance;
    }

    @Override
    public Type type() {
        return Type.GENERAL;
    }

}
