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
package org.apache.seata.saga.statelang.domain;

import java.util.Map;

/**
 * A State in StateMachine
 *
 */
public interface State {

    /**
     * name
     *
     * @return the state name
     */
    String getName();

    /**
     * comment
     *
     * @return the state comment
     */
    String getComment();

    /**
     * type
     *
     * @return the state type
     */
    StateType getType();

    /**
     * next state name
     *
     * @return the next state name
     */
    String getNext();

    /**
     * extension properties
     *
     * @return the state extensions
     */
    Map<String, Object> getExtensions();

    /**
     * state machine instance
     *
     * @return the state machine
     */
    StateMachine getStateMachine();
}
