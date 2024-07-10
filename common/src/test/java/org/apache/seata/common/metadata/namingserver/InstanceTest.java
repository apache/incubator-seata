
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
package org.apache.seata.common.metadata.namingserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.metadata.Node;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InstanceTest {

    @Test
    void toJsonString() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Instance instance = Instance.getInstance();
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> mmap = new HashMap<>();
        mmap.put("k","v");
        map.put("k",mmap);
        instance.setMetadata(map);
        instance.setControlEndpoint(new Node.Endpoint("1.1.1.1",888));
        instance.setTransactionEndpoint(new Node.Endpoint("2.2.2.2",999));
        assertEquals(instance.toJsonString(),objectMapper.writeValueAsString(instance));
    }
}