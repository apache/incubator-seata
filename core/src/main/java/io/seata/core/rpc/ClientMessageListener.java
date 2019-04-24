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
package io.seata.core.rpc;

/**
 * The interface Client message listener.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/10
 */
public interface ClientMessageListener {
    /**
     * On message.
     *
     * @param msgId         the msg id
     * @param serverAddress the server address
     * @param msg           the msg
     * @param sender        the sender
     */
    void onMessage(long msgId, String serverAddress, Object msg, ClientMessageSender sender);
}
