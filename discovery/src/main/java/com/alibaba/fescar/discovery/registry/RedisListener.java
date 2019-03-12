/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.discovery.registry;

/**
 * The RedisListener
 *
 * @author kl @kailing.pub
 * @date 2019 /2/27
 */
public interface RedisListener {
    /**
     * The constant REGISTER.
     */
    String REGISTER = "register";
    /**
     * The constant UN_REGISTER.
     */
    String UN_REGISTER = "unregister";

    /**
     * 用于订阅redis事件
     *
     * @param event the event
     */
    void onEvent(String event);
}