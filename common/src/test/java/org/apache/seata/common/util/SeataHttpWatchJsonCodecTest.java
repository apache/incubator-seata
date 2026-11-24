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
package org.apache.seata.common.util;

import okhttp3.ResponseBody;
import org.apache.seata.common.json.TestingJsonCodec;
import org.apache.seata.common.metadata.ClusterWatchEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeataHttpWatchJsonCodecTest {

    @Test
    public void testNext_UsesJsonCodecForEventParsing() throws Exception {
        String sseData = "CW:codec:group=json-codec-test,timestamp=1234567890,term=7\n";
        SeataHttpWatch<ClusterWatchEvent> watch = newWatch(sseData);

        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type);
        assertEquals("json-codec-test", response.object.getGroup());
        assertEquals(7L, response.object.getMetadata().getTerm());
        assertTrue(TestingJsonCodec.SERIALIZED_PREFIX.contains("testing"));
    }

    @SuppressWarnings("unchecked")
    private static SeataHttpWatch<ClusterWatchEvent> newWatch(String body) throws Exception {
        Constructor<SeataHttpWatch> constructor =
                SeataHttpWatch.class.getDeclaredConstructor(ResponseBody.class, okhttp3.Call.class, Class.class);
        constructor.setAccessible(true);
        return constructor.newInstance(ResponseBody.create(body, null), null, ClusterWatchEvent.class);
    }
}
