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
package org.apache.seata.saga.statelang.parser.impl;

import java.util.List;
import java.util.Map;

import org.apache.seata.saga.statelang.domain.ServiceTaskState;
import org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;
import org.apache.seata.saga.statelang.parser.StateParser;

/**
 * ServiceTaskTask parser
 *
 */
public class ServiceTaskStateParser extends AbstractTaskStateParser implements StateParser<ServiceTaskState> {

    @Override
    public ServiceTaskState parse(Object node) {

        ServiceTaskStateImpl serviceTaskState = new ServiceTaskStateImpl();

        parseTaskAttributes(serviceTaskState, node);

        Map<String, Object> nodeMap = (Map<String, Object>)node;
        serviceTaskState.setServiceName((String)nodeMap.get("ServiceName"));
        serviceTaskState.setServiceMethod((String)nodeMap.get("ServiceMethod"));
        serviceTaskState.setServiceType((String)nodeMap.get("ServiceType"));
        serviceTaskState.setParameterTypes((List<String>)nodeMap.get("ParameterTypes"));
        Object isAsync = nodeMap.get("IsAsync");
        if (Boolean.TRUE.equals(isAsync)) {
            serviceTaskState.setAsync(true);
        }

        return serviceTaskState;
    }
}
