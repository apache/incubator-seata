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
package io.seata.common.exception;

/**
 * The enum Framework error code.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/9
 */
public enum FrameworkErrorCode {
    /**
     * 0001 ~ 0099  Configuration related errors
     */
    ThreadPoolFull("0004", "Thread pool is full", "Please check the thread pool configuration"),

    /**
     * The Init services client error.
     */
    InitSeataClientError("0008", "Seata app name or seata server group is null", "Please check your configuration"),

    /**
     * The Null rule error.
     */
    NullRuleError("0010", "Services rules is null", "Please check your configuration"),

    /**
     * 0101 ~ 0199 Network related error. (Not connected, disconnected, dispatched, etc.)
     */
    NetConnect("0101", "Can not connect to the server", "Please check if the seata service is started. Is the network connection to the seata server normal?"),

    /**
     * The Net reg appname.
     */
    NetRegAppname("0102", "Register client app name failed", "Please check if the seata service is started. Is the network connection to the seata server normal?"),

    /**
     * The Net disconnect.
     */
    NetDisconnect("0103", "Seata connection closed", "The network is disconnected. Please check the network connection to the client or seata server."),

    /**
     * The Net dispatch.
     */
    NetDispatch("0104", "Dispatch error", "Network processing error. Please check the network connection to the client or seata server."),

    /**
     * The Net on message.
     */
    NetOnMessage("0105", "On message error", "Network processing error. Please check the network connection to the client or seata server."),
    /**
     * Get channel error framework error code.
     */
    getChannelError("0106", "Get channel error", "Get channel error"),

    /**
     * Channel not writable framework error code.
     */
    ChannelNotWritable("0107", "Channel not writable", "Channel not writable"),

    /**
     * Send half message failed framework error code.
     */
    SendHalfMessageFailed("0108", "Send half message failed", "Send half message failed"),

    /**
     * Channel is not writable framework error code.
     */
    ChannelIsNotWritable("0109", "Channel is not writable", "Channel is not writable"),
    /**
     * No available service framework error code.
     */
    NoAvailableService("0110", "No available service", "No available service"),

    /**
     * Invalid configuration framework error code.
     */
    InvalidConfiguration("0201", "Invalid configuration", "Invalid configuration"),

    /**
     * Exception caught framework error code.
     */
    ExceptionCaught("0318", "Exception caught", "Exception caught"),

    /**
     * Register rm framework error code.
     */
    RegisterRM("0304", "Register RM failed", "Register RM failed"),

    /**
     * Undefined error
     */
    UnknownAppError("10000", "Unknown error", "Internal error"),
    ;

    /**
     * The Err code.
     */
    private String errCode;

    /**
     * The Err message.
     */
    private String errMessage;

    /**
     * The Err dispose.
     */
    private String errDispose;

    FrameworkErrorCode(String errCode, String errMessage, String errDispose) {
        this.errCode = errCode;
        this.errMessage = errMessage;
        this.errDispose = errDispose;
    }

    public String getErrCode() {
        return errCode;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public String getErrDispose() {
        return errDispose;
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s]", errCode, errMessage, errDispose);
    }
}
