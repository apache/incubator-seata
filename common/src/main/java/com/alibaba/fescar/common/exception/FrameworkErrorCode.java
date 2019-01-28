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

package com.alibaba.fescar.common.exception;

/**
 * The enum Framework error code.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /10/9 15:37
 * @FileName: FrameworkErrorCode
 * @Description:
 */
public enum FrameworkErrorCode {
    /**
     * 0001 ~ 0099  与配置相关的错误
     */
    ThreadPoolFull("0004", "netty线程池满", "请在配置文件中调整线程数， corePoolSize 的值调大一些"),
    /**
     * The Init fescar client error.
     */
    InitFescarClientError("0008", "fescarAppName or fescarServerGroup is null", ""),
    /**
     * The Null rule error.
     */
    NullRuleError("0010", "fescar rules is null", ""),

    /**
     * 0101 ~ 0199 网络有关的错误，连接不上，断开，dispatch等
     */
    NetConnect("0101", "无法连接服务器", "请检查fescar server是否启动，到fescar server的网络连接是否正常"),
    /**
     * The Net reg appname.
     */
    NetRegAppname("0102", "register client app name failed", "请检查fescar server是否启动，到fescar server的网络连接是否正常"),
    /**
     * The Net disconnect.
     */
    NetDisconnect("0103", "fescarConnection closed", "网络断开，请检查到对端（client 或fescar server）的网络连接"),
    /**
     * The Net dispatch.
     */
    NetDispatch("0104", "dispatch 错误", "网络处理错误，请检查到对端（client 或fescar server）的网络连接 "),
    /**
     * The Net on message.
     */
    NetOnMessage("0105", "on message 错误", "网络处理错误，请检查到对端（client 或fescar server）的网络连接 "),
    /**
     * Get channel error framework error code.
     */
    getChannelError("0106", "getChannelError", "getChannelError"),
    /**
     * Channel not writable framework error code.
     */
    ChannelNotWritable("0107", "ChannelNotWritable", "ChannelNotWritable"),
    /**
     * Send half message failed framework error code.
     */
    SendHalfMessageFailed("0108", "SendHalfMessageFailed", "SendHalfMessageFailed"),

    /**
     * Channel is not writable framework error code.
     */
    ChannelIsNotWritable("0109", "ChannelIsNotWritable", "ChannelIsNotWritable"),
    /**
     * No available service framework error code.
     */
    NoAvailableService("0110", "NoAvailableService", "NoAvailableService"),

    /**
     * Invalid configuration framework error code.
     */
    InvalidConfiguration("0201", "InvalidConfiguration", "InvalidConfiguration"),

    /**
     * Exception caught framework error code.
     */
    ExceptionCaught("0318", "异常", ""),
    /**
     * Regist rm framework error code.
     */
    RegistRM("0304", "注册RM失败", ""),

    /**
     * 未定义错误
     */
    UnknownAppError("10000","unknown error","内部错误"),
    ;

    /**
     * The Err code.
     */
    public String errCode;
    /**
     * The Err message.
     */
    public String errMessage;
    /**
     * The Err dispose.
     */
    public String errDispose;

    FrameworkErrorCode(String errCode, String errMessage, String errDispose) {
        this.errCode = errCode;
        this.errMessage = errMessage;
        this.errDispose = errDispose;
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s]", errCode, errMessage, errDispose);
    }
}
