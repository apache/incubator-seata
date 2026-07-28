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

import org.apache.seata.core.protocol.UnregisterRMRequest;
import org.apache.seata.serializer.protobuf.generated.AbstractIdentifyRequestProto;
import org.apache.seata.serializer.protobuf.generated.AbstractMessageProto;
import org.apache.seata.serializer.protobuf.generated.MessageTypeProto;
import org.apache.seata.serializer.protobuf.generated.UnregisterRMRequestProto;

public class UnregisterRMRequestConvertor implements PbConvertor<UnregisterRMRequest, UnregisterRMRequestProto> {
    @Override
    public UnregisterRMRequestProto convert2Proto(UnregisterRMRequest unregisterRMRequest) {
        final short typeCode = unregisterRMRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder()
                .setMessageType(MessageTypeProto.forNumber(typeCode))
                .build();

        final String extraData = unregisterRMRequest.getExtraData();
        AbstractIdentifyRequestProto abstractIdentifyRequestProto = AbstractIdentifyRequestProto.newBuilder()
                .setAbstractMessage(abstractMessage)
                .setApplicationId(unregisterRMRequest.getApplicationId())
                .setExtraData(extraData == null ? "" : extraData)
                .setTransactionServiceGroup(unregisterRMRequest.getTransactionServiceGroup())
                .setVersion(unregisterRMRequest.getVersion())
                .build();
        UnregisterRMRequestProto result = UnregisterRMRequestProto.newBuilder()
                .setAbstractIdentifyRequest(abstractIdentifyRequestProto)
                .setResourceIds(
                        unregisterRMRequest.getResourceIds() == null ? "" : unregisterRMRequest.getResourceIds())
                .build();

        return result;
    }

    @Override
    public UnregisterRMRequest convert2Model(UnregisterRMRequestProto unregisterRMRequestProto) {
        UnregisterRMRequest unregisterRMRequest = new UnregisterRMRequest();

        AbstractIdentifyRequestProto abstractIdentifyRequest = unregisterRMRequestProto.getAbstractIdentifyRequest();
        unregisterRMRequest.setResourceIds(unregisterRMRequestProto.getResourceIds());
        unregisterRMRequest.setApplicationId(abstractIdentifyRequest.getApplicationId());
        unregisterRMRequest.setExtraData(abstractIdentifyRequest.getExtraData());
        unregisterRMRequest.setTransactionServiceGroup(abstractIdentifyRequest.getTransactionServiceGroup());
        unregisterRMRequest.setVersion(abstractIdentifyRequest.getVersion());

        return unregisterRMRequest;
    }
}
