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
package io.seata.serializer.seata.protocol.v0;

import io.seata.core.protocol.MessageType;
import io.seata.serializer.seata.MessageCodecFactory;
import io.seata.serializer.seata.MessageSeataCodec;
import io.seata.serializer.seata.protocol.BatchResultMessageCodec;
import io.seata.serializer.seata.protocol.MergeResultMessageCodec;
import io.seata.serializer.seata.protocol.MergedWarpMessageCodec;
import io.seata.serializer.seata.protocol.RegisterRMRequestCodec;
import io.seata.serializer.seata.protocol.RegisterRMResponseCodec;
import io.seata.serializer.seata.protocol.RegisterTMRequestCodec;
import io.seata.serializer.seata.protocol.RegisterTMResponseCodec;
import io.seata.serializer.seata.protocol.transaction.BranchCommitRequestCodec;
import io.seata.serializer.seata.protocol.transaction.BranchRollbackRequestCodec;
import io.seata.serializer.seata.protocol.transaction.GlobalReportRequestCodec;

/**
 * The type Message codec factory v0.
 */
public class MessageCodecFactoryV0 extends MessageCodecFactory {

    /**
     * Gets msg instance by code.
     *
     * @param typeCode the type code
     * @return the msg instance by code
     */
    public MessageSeataCodec getMessageCodec(short typeCode) {
        MessageSeataCodec msgCodec = null;
        switch (typeCode) {
            case MessageType.TYPE_SEATA_MERGE:
                msgCodec = new MergedWarpMessageCodec();
                break;
            case MessageType.TYPE_SEATA_MERGE_RESULT:
                msgCodec = new MergeResultMessageCodec();
                break;
            case MessageType.TYPE_REG_CLT:
                msgCodec = new RegisterTMRequestCodec();
                break;
            case MessageType.TYPE_REG_CLT_RESULT:
                msgCodec = new RegisterTMResponseCodec();
                break;
            case MessageType.TYPE_REG_RM:
                msgCodec = new RegisterRMRequestCodec();
                break;
            case MessageType.TYPE_REG_RM_RESULT:
                msgCodec = new RegisterRMResponseCodec();
                break;
            case MessageType.TYPE_BRANCH_COMMIT:
                msgCodec = new BranchCommitRequestCodec();
                break;
            case MessageType.TYPE_BRANCH_ROLLBACK:
                msgCodec = new BranchRollbackRequestCodec();
                break;
            case MessageType.TYPE_GLOBAL_REPORT:
                msgCodec = new GlobalReportRequestCodec();
                break;
            case MessageType.TYPE_BATCH_RESULT_MSG:
                msgCodec = new BatchResultMessageCodec();
                break;
            default:
                break;
        }

        return msgCodec;
    }
}
