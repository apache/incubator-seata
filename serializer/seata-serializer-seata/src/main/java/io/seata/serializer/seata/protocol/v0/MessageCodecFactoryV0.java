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
package io.seata.serializer.seata.protocol.v0;

import io.seata.core.protocol.MessageType;
import io.seata.serializer.seata.MessageSeataCodec;
import io.seata.serializer.seata.protocol.v1.MessageCodecFactoryV1;

/**
 * The type Message codec factory v0.
 *
 * @author Bughue
 */
public class MessageCodecFactoryV0 extends MessageCodecFactoryV1 {

    public MessageSeataCodec getMessageCodec(short typeCode) {
        MessageSeataCodec msgCodec = null;
        switch (typeCode) {
            case MessageType.TYPE_SEATA_MERGE:
                msgCodec = new MergedWarpMessageCodec();
                break;
            case MessageType.TYPE_SEATA_MERGE_RESULT:
                msgCodec = new MergeResultMessageCodec();
                break;
            case MessageType.TYPE_BATCH_RESULT_MSG:
                msgCodec = new BatchResultMessageCodec();
                break;
            default:
                break;
        }

        if (msgCodec != null) {
            return msgCodec;
        }

        return super.getMessageCodec(typeCode);
    }
}
