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

import org.apache.seata.serializer.protobuf.generated.AbstractMessageProto;
import org.apache.seata.serializer.protobuf.generated.AbstractTransactionRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalBeginRequestProto;
import org.apache.seata.serializer.protobuf.generated.MessageTypeProto;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;


public class GlobalBeginRequestConvertor implements PbConvertor<GlobalBeginRequest, GlobalBeginRequestProto> {

    @Override
    public GlobalBeginRequestProto convert2Proto(GlobalBeginRequest globalBeginRequest) {
        final short typeCode = globalBeginRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(abstractMessage).build();

        GlobalBeginRequestProto result = GlobalBeginRequestProto.newBuilder().setTimeout(
            globalBeginRequest.getTimeout()).setTransactionName(globalBeginRequest.getTransactionName())
            .setAbstractTransactionRequest(abstractTransactionRequestProto).build();
        return result;
    }

    @Override
    public GlobalBeginRequest convert2Model(GlobalBeginRequestProto globalBeginRequestProto) {
        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTimeout(globalBeginRequestProto.getTimeout());
        globalBeginRequest.setTransactionName(globalBeginRequestProto.getTransactionName());
        return globalBeginRequest;
    }
}
