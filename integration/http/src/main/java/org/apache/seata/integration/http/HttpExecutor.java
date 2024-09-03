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
package org.apache.seata.integration.http;

import java.io.IOException;
import java.util.Map;

/**
 * Http executor.
 *
 */
public interface HttpExecutor {

    /**
     * Execute post k.
     *
     * @param <T>         the type parameter
     * @param <K>         the type parameter
     * @param host        the host
     * @param path        the path
     * @param paramObject the param object
     * @param returnType  the return type
     * @return the k
     * @throws IOException the io exception
     */
    <T, K> K executePost(String host, String path, T paramObject, Class<K> returnType) throws IOException;

    /**
     * get method only support param type of Map<String,String>
     *
     * @param <K>         the type parameter
     * @param host        the host
     * @param path        the path
     * @param paramObject the param object
     * @param returnType  the return type
     * @return K k
     * @throws IOException the io exception
     */
    <K> K executeGet(String host, String path, Map<String, String> paramObject, Class<K> returnType) throws IOException;

    /**
     * Execute put k.
     *
     * @param <T>         the type parameter
     * @param <K>         the type parameter
     * @param host        the host
     * @param path        the path
     * @param paramObject the param object
     * @param returnType  the return type
     * @return the k
     * @throws IOException the io exception
     */
    <T, K> K executePut(String host, String path, T paramObject, Class<K> returnType) throws IOException;

}
