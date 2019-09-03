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
package io.seata.saga.engine;

import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.exception.ForwardInvalidException;
import io.seata.saga.statelang.domain.StateMachineInstance;
import java.util.Map;

/**
 * StateMachineEngine
 *
 * @author lorne.cl
 */
public interface StateMachineEngine {

    /**
     * 启动状态机
     * @param stateMachineName 状态机定义名，如果有多个版本，则执行最新版本
     * @param startParams 启动参数
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance start(String stateMachineName, Map<String, Object> startParams) throws EngineExecutionException;

    /**
     * 启动状态机(带业务主键)
     * @param stateMachineName 状态机定义名，如果有多个版本，则执行最新版本
     * @param businessKey 业务主键,如流水号
     * @param startParams 启动参数
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance startWithBusinessKey(String stateMachineName, String businessKey, Map<String, Object> startParams) throws EngineExecutionException;

    /**
     * 启动状态机，采用整件驱动架构，每一个state都是异步执行的
     * @param stateMachineName 状态机定义名，如果有多个版本，则执行最新版本
     * @param startParams 启动参数
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance startAsync(String stateMachineName, Map<String, Object> startParams, AsyncCallback callback) throws EngineExecutionException;

    /**
     * 异步启动状态机，采用整件驱动架构，每一个state都是异步执行的(带业务主键)
     * @param stateMachineName 状态机定义名，如果有多个版本，则执行最新版本
     * @param businessKey 业务主键,如流水号
     * @param startParams 启动参数
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance startWithBusinessKeyAsync(String stateMachineName, String businessKey, Map<String, Object> startParams, AsyncCallback callback) throws EngineExecutionException;

    /**
     * 恢复出错状态的状态机实例，让其往前执行
     * @param stateMachineInstId 状态机实例编号
     * @param replaceParams 用来替换(订正)上下文参数中相同名称的参数(非必输)
     * @throws EngineExecutionException
     */
    StateMachineInstance forward(String stateMachineInstId, Map<String, Object> replaceParams) throws ForwardInvalidException;

    /**
     * 恢复出错状态的状态机实例，让其往前执行（异步执行）
     * @param stateMachineInstId 状态机实例编号
     * @param replaceParams 用来替换(订正)上下文参数中相同名称的参数(非必输)
     * @throws EngineExecutionException
     */
    StateMachineInstance forwardAsync(String stateMachineInstId, Map<String, Object> replaceParams, AsyncCallback callback) throws ForwardInvalidException;

    /**
     * 反向补偿有数据不一致性状态的状态机实例（异步执行）
     * @param stateMachineInstId 状态机实例编号
     * @param replaceParams 用来替换状态机上下相同名称的参数(非必输)
     * @throws EngineExecutionException
     */
    StateMachineInstance compensate(String stateMachineInstId, Map<String, Object> replaceParams) throws EngineExecutionException;

    /**
     * 反向补偿有数据不一致性状态的状态机实例（异步执行）
     * @param stateMachineInstId 状态机实例编号
     * @param replaceParams 用来替换状态机上下相同名称的参数(非必输)
     * @throws EngineExecutionException
     */
    StateMachineInstance compensateAsync(String stateMachineInstId, Map<String, Object> replaceParams, AsyncCallback callback) throws EngineExecutionException;

    /**
     * 跳过当前失败的state节点，继续往前执行
     * @param stateMachineInstId 状态机实例编号
     * @throws EngineExecutionException
     */
    StateMachineInstance skipAndForward(String stateMachineInstId) throws EngineExecutionException;

    /**
     * 跳过当前失败的state节点，继续往前执行（异步执行）
     * @param stateMachineInstId 状态机实例编号
     * @throws EngineExecutionException
     */
    StateMachineInstance skipAndForwardAsync(String stateMachineInstId, AsyncCallback callback) throws EngineExecutionException;

    /**
     * 获取引擎的参数设置
     * @return
     */
    StateMachineConfig getStateMachineConfig();
}