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
package io.seata.saga.engine.sequence;

import java.util.List;

import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

/**
 * Based On Spring AlternativeJdkIdGenerator
 *
 * @author lorne.cl
 */
public class SpringJvmUUIDSeqGenerator implements SeqGenerator {

    private IdGenerator idGenerator = new AlternativeJdkIdGenerator();

    @Override
    public String generate(String entity, String ruleName, List<Object> shardingParameters) {
        String uuid = idGenerator.generateId().toString();
        StringBuffer buf = new StringBuffer(uuid.length() - 4);
        for (String seg : uuid.split("-")) {
            buf.append(seg);
        }
        return buf.toString();
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