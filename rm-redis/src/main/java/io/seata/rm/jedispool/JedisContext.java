/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.jedispool;

import static io.seata.common.Constants.REDIS_TX_LOG;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.rm.context.ATContext;

/**
 * The type Connection context.
 *
 * @author funkye
 */
public class JedisContext extends ATContext {

    /**
     * the undo items buffer
     */
    private final List<KVUndolog> kvUndoItemsBuffer = new ArrayList<>();

    /**
     * Append undo item.
     *
     * @param kvUndolog the kv undo log
     */
    void appendUndoItem(KVUndolog kvUndolog) {
        kvUndoItemsBuffer.add(kvUndolog);
    }

    /**
     * Has undo log boolean.
     *
     * @return the boolean
     */
    public boolean hasUndoLog() {
        return !kvUndoItemsBuffer.isEmpty();
    }

    /**
     * Gets applicationData.
     *
     * @return the application data
     */
    public String getApplicationData() throws TransactionException {
        try {
            applicationData.put(REDIS_TX_LOG, MAPPER.writeValueAsString(getUndoItems()));
            return MAPPER.writeValueAsString(applicationData);
        } catch (JsonProcessingException e) {
            throw new TransactionException(e.getMessage(), e);
        }
    }

    public JedisContext reloadApplicationData(String applicationData) throws TransactionException {
        try {
            this.applicationData.putAll(MAPPER.readValue(applicationData, new TypeReference<Map<String, String>>() {
            }));
            String txLog = (String)this.applicationData.get(REDIS_TX_LOG);
            if (StringUtils.isNotBlank(txLog)) {
                CollectionType javaType = MAPPER.getTypeFactory().constructCollectionType(List.class, KVUndolog.class);
                this.kvUndoItemsBuffer.addAll(MAPPER.readValue(txLog, javaType));
            }
        } catch (JsonProcessingException e) {
            throw new TransactionException(e.getMessage(), e);
        }
        return this;
    }

    /**
     * Gets undo items.
     *
     * @return the undo items
     */
    public List<KVUndolog> getUndoItems() {
        return kvUndoItemsBuffer;
    }

    /**
     * Check whether all the before image is empty.
     *
     * @return if all is empty, return true
     */
    private boolean allBeforeImageEmpty() {
        for (KVUndolog undoLog : kvUndoItemsBuffer) {
            if (StringUtils.isNotBlank(undoLog.getBeforeValue())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    @Override
    public void reset() {
        super.reset();
        kvUndoItemsBuffer.clear();
    }

}
