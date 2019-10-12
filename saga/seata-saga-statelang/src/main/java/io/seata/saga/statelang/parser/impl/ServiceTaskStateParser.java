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
package io.seata.saga.statelang.parser.impl;

import io.seata.saga.statelang.domain.ServiceTaskState;
import io.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;
import io.seata.saga.statelang.parser.StateParser;

import java.util.List;
import java.util.Map;

/**
 * ServcieTaskTask parser
 * @author lorne.cl
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

        return serviceTaskState;
    }
}