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
package io.seata.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import io.seata.common.exception.ShouldNeverHappenException;

/**
 * The type Uuid generator.
 *
 * @author sharajava
 */
public class UUIDGenerator {

    private static final AtomicLong UUID = new AtomicLong(1000);
    private static int serverNodeId = 1;
    private static final long UUID_INTERNAL = 2000000000;
    private static long initUUID = 0;

    /**
     * Generate uuid long.
     *
     * @return the long
     */
    public static long generateUUID() {
        long id = UUID.incrementAndGet();
        if (id >= getMaxUUID()) {
            synchronized (UUID) {
                if (UUID.get() >= id) {
                    id -= UUID_INTERNAL;
                    UUID.set(id);
                }
            }
        }
        return id;
    }

    /**
     * Gets current uuid.
     *
     * @return the current uuid
     */
    public static long getCurrentUUID() {
        return UUID.get();
    }

    /**
     * Sets uuid.
     *
     * @param expect the expect
     * @param update the update
     * @return the uuid
     */
    public static boolean setUUID(long expect, long update) {
        return UUID.compareAndSet(expect, update);

    }

    /**
     * Gets max uuid.
     *
     * @return the max uuid
     */
    public static long getMaxUUID() {
        return UUID_INTERNAL * (serverNodeId + 1);
    }

    /**
     * Gets init uuid.
     *
     * @return the init uuid
     */
    public static long getInitUUID() {
        return initUUID;
    }

    /**
     * Init.
     *
     * @param serverNodeId the server node id
     */
    public static void init(int serverNodeId) {
        try {
            UUIDGenerator.serverNodeId = serverNodeId;
            UUID.set(UUID_INTERNAL * serverNodeId);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();
            Date date = format.parse("2019-01-01");
            cal.setTime(date);
            long base = cal.getTimeInMillis();
            long current = System.currentTimeMillis();
            UUID.addAndGet((current - base) / 1000);
            initUUID = getCurrentUUID();
        } catch (ParseException e) {
            throw new ShouldNeverHappenException(e);
        }
    }
}
