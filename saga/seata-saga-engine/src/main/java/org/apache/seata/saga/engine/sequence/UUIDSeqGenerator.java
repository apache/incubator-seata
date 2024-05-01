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
package org.apache.seata.saga.engine.sequence;

import java.util.List;
import java.util.UUID;

/**
 * UUID SeqGenerator
 *
 */
public class UUIDSeqGenerator implements SeqGenerator {

    @Override
    public String generate(String entity, String ruleName, List<Object> shardingParameters) {
        String uuid = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder(uuid.length() - 4);
        for (String seg : uuid.split("-")) {
            sb.append(seg);
        }
        return sb.toString();
    }

    @Override
    public String generate(String entity, List<Object> shardingParameters) {
        return generate(entity, null, shardingParameters);
    }

    @Override
    public String generate(String entity) {
        return generate(entity, null);
    }
}
