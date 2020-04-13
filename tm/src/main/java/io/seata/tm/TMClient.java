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
package io.seata.tm;

import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.netty.TmNettyClient;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.core.rpc.processor.Pair;
import io.seata.core.rpc.processor.client.ClientHeartbeatProcessor;
import io.seata.core.rpc.processor.client.ClientOnResponseProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Tm client.
 *
 * @author slievrly
 */
public class TMClient {

    /**
     * Init.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public static void init(String applicationId, String transactionServiceGroup) {
        TmNettyClient tmNettyClient = TmNettyClient.getInstance(applicationId, transactionServiceGroup);

        Map<Integer, Pair<RemotingProcessor, Boolean>> processorMap = new HashMap<>();
        // on response processor
        Pair<RemotingProcessor, Boolean> onResponseProcessor =
            new Pair<>(new ClientOnResponseProcessor(tmNettyClient.getMergeMsgMap(), tmNettyClient.getFutures(), null), false);
        processorMap.put((int) MessageType.TYPE_SEATA_MERGE_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_GLOBAL_BEGIN_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_GLOBAL_COMMIT_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_GLOBAL_REPORT_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_GLOBAL_ROLLBACK_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_GLOBAL_STATUS_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_REG_CLT_RESULT, onResponseProcessor);

        // heartbeat message processor
        Pair<RemotingProcessor, Boolean> heartbeatMessageProcessor = new Pair<>(new ClientHeartbeatProcessor(), false);
        processorMap.put((int) MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor);
        tmNettyClient.setTmProcessor(processorMap);

        tmNettyClient.init();
    }

}
