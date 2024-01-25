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
package org.apache.seata.server.storage.redis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.io.FileLoader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisNoScriptException;

/**
 * lua related utils
 *
 */
public class LuaParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuaParser.class);

    private static final String WHITE_SPACE = " ";

    private static final String ANNOTATION_LUA = "--";

    private static final Map<String, String> LUA_FILE_MAP = new HashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public final static class LuaResult implements Serializable {
        private static final long serialVersionUID = -4160065043902060730L;
        private Boolean success;
        private String status;
        private String data;

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @Override public String toString() {
            return "LuaResult{" +
                "success=" + success +
                ", type='" + status + '\'' +
                ", data='" + data + '\'' +
                '}';
        }
    }

    public final static class LuaErrorStatus {

        public static final String ANOTHER_ROLLBACKING = "AnotherRollbackIng";

        public static final String ANOTHER_HOLDING = "AnotherHoldIng";

        public static final String XID_NOT_EXISTED = "NotExisted";

        public static final String ILLEGAL_CHANGE_STATUS = "ChangeStatusFail";
    }

    /**
     * get lua string from lua file.
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Map<String, String> getEvalShaMapFromFile(String fileName) throws IOException {
        File luaFile = FileLoader.load(fileName);
        if (luaFile == null) {
            throw new IOException("no lua file: " + fileName);
        }
        StringBuilder luaByFile = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(luaFile)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith(ANNOTATION_LUA)) {
                    continue;
                }
                luaByFile.append(line);
                luaByFile.append(WHITE_SPACE);
            }
        } catch (IOException e) {
            throw new IOException(e);
        }
        LUA_FILE_MAP.put(fileName, luaByFile.toString());
        Map<String, String> resultMap = new ConcurrentHashMap<>(1);
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            resultMap.put(fileName, jedis.scriptLoad(luaByFile.toString()));
            return resultMap;
        } catch (UnsupportedOperationException | JedisDataException e) {
            throw new IOException(e);
        }
    }

    public static <T> T getObjectFromJson(String json, Class<T> classz) {
        try {
            return OBJECT_MAPPER.readValue(json, classz);
        } catch (JsonProcessingException e) {
            throw new StoreException(e.getMessage());
        }
    }

    public static <T> List<T> getListFromJson(String json, Class<T> classz) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, new TypeReference<List<T>>() {
            });
        } catch (JsonProcessingException e) {
            throw new StoreException(e.getMessage());
        }
    }

    public static Object jedisEvalSha(Jedis jedis, String luaSHA, String luaFileName, List<String> keys, List<String> args) {
        try {
            return jedis.evalsha(luaSHA, keys, args);
        } catch (JedisNoScriptException e) {
            LOGGER.warn("try to reload the lua script and execute,jedis ex: " + e.getMessage());
            jedis.scriptLoad(LUA_FILE_MAP.get(luaFileName));
            return jedis.evalsha(luaSHA, keys, args);
        }
    }
}
