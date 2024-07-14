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
package org.apache.seata.config.raft;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationProvider;

import static org.apache.seata.common.Constants.*;

@LoadLevel(name = "Raft", order = 1)
public class RaftConfigurationProvider implements ConfigurationProvider {
    @Override
    public Configuration provide() {
        String applicationType = System.getProperty(APPLICATION_TYPE_KEY);
        if (APPLICATION_TYPE_SERVER.equals(applicationType)){
            return RaftConfigurationServer.getInstance();
        }else if (APPLICATION_TYPE_CLIENT.equals(applicationType)){
            return RaftConfigurationClient.getInstance();
        }else{
            throw new IllegalArgumentException(String.format("Unknown application type: %s, it must be server or client", applicationType));
        }
    }
}
