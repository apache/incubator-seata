/*
 *  Copyright 1999-2023 Liber Group.
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
package io.seata.server.cluster.raft.serializer;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.loader.LoadLevel;
import io.seata.core.serializer.Serializer;

/**
 * @author jianbin.chen
 */
@LoadLevel(name = "JACKSON")
public class JacksonSerializer implements Serializer {

	ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> byte[] serialize(T t) {
        try {
            JsonInfo jsonInfo = new JsonInfo(objectMapper.writeValueAsBytes(t), t.getClass());
            return objectMapper.writeValueAsBytes(jsonInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        try {
            JsonInfo jsonInfo = objectMapper.readValue(bytes, JsonInfo.class);
            return (T)objectMapper.readValue(jsonInfo.getObj(), jsonInfo.getClz());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	static class JsonInfo {
		
		byte[] obj;
		
		Class<?> clz;

		public JsonInfo() {
		}

		public JsonInfo(byte[] obj, Class<?> clz) {
			this.obj = obj;
			this.clz = clz;
		}

		public byte[] getObj() {
			return obj;
		}

		public void setObj(byte[] obj) {
			this.obj = obj;
		}

		public Class<?> getClz() {
			return clz;
		}

		public void setClz(Class<?> clz) {
			this.clz = clz;
		}
	}
	
}
