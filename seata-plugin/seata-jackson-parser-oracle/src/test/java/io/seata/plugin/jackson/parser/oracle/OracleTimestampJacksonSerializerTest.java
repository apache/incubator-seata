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
package org.apache.seata.plugin.jackson.parser.oracle;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.rm.datasource.undo.parser.spi.JacksonSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


public class OracleTimestampJacksonSerializerTest {

    @Test
    public void test_oracleJacksonSerializer() throws Exception {
        List<JacksonSerializer> serializers = EnhancedServiceLoader.loadAll(JacksonSerializer.class);
        Assertions.assertTrue(serializers.size() > 0, "Jackson Serializer is empty");
        OracleTimestampJacksonSerializer s = null;
        for (JacksonSerializer serializer : serializers) {
            if (serializer instanceof OracleTimestampJacksonSerializer) {
                s = (OracleTimestampJacksonSerializer) serializer;
                break;
            }
        }
        Assertions.assertNotNull(s);
    }

}
