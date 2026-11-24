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
package org.apache.seata.serializer.protobuf.convertor;

import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.UnregisterRMResponse;
import org.apache.seata.serializer.protobuf.generated.AbstractIdentifyResponseProto;
import org.apache.seata.serializer.protobuf.generated.AbstractMessageProto;
import org.apache.seata.serializer.protobuf.generated.AbstractResultMessageProto;
import org.apache.seata.serializer.protobuf.generated.MessageTypeProto;
import org.apache.seata.serializer.protobuf.generated.ResultCodeProto;
import org.apache.seata.serializer.protobuf.generated.UnregisterRMResponseProto;

public class UnregisterRMResponseConvertor implements PbConvertor<UnregisterRMResponse, UnregisterRMResponseProto> {
    @Override
    public UnregisterRMResponseProto convert2Proto(UnregisterRMResponse unregisterRMResponse) {
        final short typeCode = unregisterRMResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder()
                .setMessageType(MessageTypeProto.forNumber(typeCode))
                .build();

        final String msg = unregisterRMResponse.getMsg();

        if (unregisterRMResponse.getResultCode() == null) {
            if (unregisterRMResponse.isIdentified()) {
                unregisterRMResponse.setResultCode(ResultCode.Success);
            } else {
                unregisterRMResponse.setResultCode(ResultCode.Failed);
            }
        }

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder()
                .setMsg(msg == null ? "" : msg)
                .setResultCode(ResultCodeProto.valueOf(
                        unregisterRMResponse.getResultCode().name()))
                .setAbstractMessage(abstractMessage)
                .build();

        final String extraData = unregisterRMResponse.getExtraData();
        AbstractIdentifyResponseProto abstractIdentifyResponseProto = AbstractIdentifyResponseProto.newBuilder()
                .setAbstractResultMessage(abstractResultMessageProto)
                .setExtraData(extraData == null ? "" : extraData)
                .setVersion(unregisterRMResponse.getVersion())
                .setIdentified(unregisterRMResponse.isIdentified())
                .build();

        UnregisterRMResponseProto result = UnregisterRMResponseProto.newBuilder()
                .setAbstractIdentifyResponse(abstractIdentifyResponseProto)
                .build();

        return result;
    }

    @Override
    public UnregisterRMResponse convert2Model(UnregisterRMResponseProto unregisterRMResponseProto) {
        UnregisterRMResponse unregisterRMResponse = new UnregisterRMResponse();

        AbstractIdentifyResponseProto abstractIdentifyResponseProto =
                unregisterRMResponseProto.getAbstractIdentifyResponse();
        unregisterRMResponse.setExtraData(abstractIdentifyResponseProto.getExtraData());
        unregisterRMResponse.setVersion(abstractIdentifyResponseProto.getVersion());
        unregisterRMResponse.setIdentified(abstractIdentifyResponseProto.getIdentified());

        unregisterRMResponse.setMsg(
                abstractIdentifyResponseProto.getAbstractResultMessage().getMsg());
        unregisterRMResponse.setResultCode(ResultCode.valueOf(abstractIdentifyResponseProto
                .getAbstractResultMessage()
                .getResultCode()
                .name()));

        return unregisterRMResponse;
    }
}
