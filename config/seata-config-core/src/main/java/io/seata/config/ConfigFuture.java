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
package io.seata.config;


import io.seata.common.exception.ShouldNeverHappenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The type Config future.
 *
 * @author slievrly
 */
public class ConfigFuture {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFuture.class);
    private static final long DEFAULT_CONFIG_TIMEOUT = 5 * 1000;
    private long timeoutMills;
    private long start = System.currentTimeMillis();
    private String dataId;
    private String content;
    private ConfigOperation operation;
    private transient CompletableFuture<Object> origin = new CompletableFuture<>();

    /**
     * Instantiates a new Config future.
     *
     * @param dataId    the data id
     * @param content   the content
     * @param operation the operation
     */
    public ConfigFuture(String dataId, String content, ConfigOperation operation) {
        this(dataId, content, operation, DEFAULT_CONFIG_TIMEOUT);
    }

    /**
     * Instantiates a new Config future.
     *
     * @param dataId       the data id
     * @param content      the content
     * @param operation    the operation
     * @param timeoutMills the timeout mills
     */
    public ConfigFuture(String dataId, String content, ConfigOperation operation, long timeoutMills) {
        this.dataId = dataId;
        this.content = content;
        this.operation = operation;
        this.timeoutMills = timeoutMills;
    }

    /**
     * Gets timeout mills.
     *
     * @return the timeout mills
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - start > timeoutMills;
    }

    /**
     * Get object.
     *
     * @return the object
     */
    public Object get() {
        return get(this.timeoutMills, TimeUnit.MILLISECONDS);
    }

    /**
     * Get object.
     *
     * @param timeout the timeout
     * @param unit    the unit
     * @return the object
     */
    public Object get(long timeout, TimeUnit unit) {
        this.timeoutMills = unit.toMillis(timeout);
        Object result;
        try {
            result = origin.get(timeout, unit);
        } catch (ExecutionException e) {
            throw new ShouldNeverHappenException("Should not get results in a multi-threaded environment", e);
        } catch (TimeoutException e) {
            LOGGER.error(
                    "config operation timeout,cost:" + (System.currentTimeMillis() - start) + " ms,op:" + operation
                            .name()
                            + ",dataId:" + dataId);
            return getFailResult();
        } catch (InterruptedException exx) {
            LOGGER.error("config operate interrupted,error:" + exx.getMessage());
            return getFailResult();
        }
        if (operation == ConfigOperation.GET) {
            return result == null ? content : result;
        } else {
            return result == null ? Boolean.FALSE : result;
        }
    }

    private Object getFailResult() {
        if (operation == ConfigOperation.GET) {
            return content;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * Sets result.
     *
     * @param result the result
     */
    public void setResult(Object result) {
        origin.complete(result);
    }

    /**
     * Gets data id.
     *
     * @return the data id
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * Sets data id.
     *
     * @param dataId the data id
     */
    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    /**
     * Gets content.
     *
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets content.
     *
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets operation.
     *
     * @return the operation
     */
    public ConfigOperation getOperation() {
        return operation;
    }

    /**
     * Sets operation.
     *
     * @param operation the operation
     */
    public void setOperation(ConfigOperation operation) {
        this.operation = operation;
    }

    /**
     * The enum Config operation.
     */
    public enum ConfigOperation {
        /**
         * Get config operation.
         */
        GET,
        /**
         * Put config operation.
         */
        PUT,
        /**
         * Putifabsent config operation.
         */
        PUTIFABSENT,
        /**
         * Remove config operation.
         */
        REMOVE
    }
}
