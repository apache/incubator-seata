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
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/10/9 15:37
 * @FileName: FrameworkErrorCode
 * @Description:
 */
public enum FrameworkErrorCode {
    /**
     * 0001 ~ 0099  与配置相关的错误
     */
    ThreadPoolFull("0004", "netty线程池满", "请在配置文件中调整线程数， corePoolSize 的值调大一些"),
    InitFescarClientError("0008", "fescarAppName or fescarServerGroup is null", ""),
    NullRuleError("0010", "fescar rules is null", ""),

    /**
     * 0101 ~ 0199 网络有关的错误，连接不上，断开，dispatch等
     */
    NetConnect("0101", "无法连接服务器", "请检查fescar server是否启动，到fescar server的网络连接是否正常"),
    NetRegAppname("0102", "register client app name failed", "请检查fescar server是否启动，到fescar server的网络连接是否正常"),
    NetDisconnect("0103", "fescarConnection closed", "网络断开，请检查到对端（client 或fescar server）的网络连接"),
    NetDispatch("0104", "dispatch 错误", "网络处理错误，请检查到对端（client 或fescar server）的网络连接 "),
    NetOnMessage("0105", "on message 错误", "网络处理错误，请检查到对端（client 或fescar server）的网络连接 "),
    getChannelError("0106", "getChannelError", "getChannelError"),
    ChannelNotWritable("0107", "ChannelNotWritable", "ChannelNotWritable"),
    SendHalfMessageFailed("0108", "SendHalfMessageFailed", "SendHalfMessageFailed"),

    ChannelIsNotWritable("0109", "ChannelIsNotWritable", "ChannelIsNotWritable"),
    NoAvailableService("0110", "NoAvailableService", "NoAvailableService"),


    InvalidConfiguration("0201", "InvalidConfiguration", "InvalidConfiguration"),

    ExceptionCaught("0318", "异常", ""),
    RegistRM("0304", "注册RM失败", ""),

    /**
     * 未定义错误
     */
    UnknownAppError("10000","unknown error","内部错误"),
    ;

    public String errCode;
    public String errMessage;
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
