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
package org.apache.seata.common.lock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The ResourceLock extends ReentrantLock and implements AutoCloseable,
 * allowing it to be used in try-with-resources blocks without needing
 * to unlock in a finally block.
 *
 * <h3>Example</h3>
 * <pre>
 * {@code
 *   private final ResourceLock resourceLock = new ResourceLock();
 *   try (ResourceLock lock = resourceLock.obtain()) {
 *     // do something while holding the resource lock
 *   }
 * }
 * </pre>
 */
public class ResourceLock extends ReentrantLock implements AutoCloseable {

    /**
     * Obtain the lock.
     *
     * @return this ResourceLock
     */
    public ResourceLock obtain() {
        lock();
        return this;
    }


    /**
     * Unlock the resource lock.
     *
     * <p>This is typically used in try-with-resources blocks to automatically
     * unlock the resource lock when the block is exited, regardless of whether
     * an exception is thrown or not.
     */
    @Override
    public void close() {
        this.unlock();
    }
}
