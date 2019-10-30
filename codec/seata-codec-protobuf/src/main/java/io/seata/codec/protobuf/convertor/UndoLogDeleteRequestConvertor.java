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
package io.seata.codec.protobuf.convertor;

import io.seata.codec.protobuf.generated.AbstractMessageProto;
import io.seata.codec.protobuf.generated.AbstractTransactionRequestProto;
import io.seata.codec.protobuf.generated.BranchTypeProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.codec.protobuf.generated.UndoLogDeleteRequestProto;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;

/**
 * @author yuanguoyao
 */
public class UndoLogDeleteRequestConvertor implements PbConvertor<UndoLogDeleteRequest, UndoLogDeleteRequestProto> {
    @Override
    public UndoLogDeleteRequestProto convert2Proto(UndoLogDeleteRequest undoLogDeleteRequest) {
        final short typeCode = undoLogDeleteRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        final UndoLogDeleteRequestProto undoLogDeleteRequestProto = UndoLogDeleteRequestProto
            .newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setSaveDays(undoLogDeleteRequest.getSaveDays())
            .setBranchType(BranchTypeProto.valueOf(undoLogDeleteRequest.getBranchType().name()))
            .setResourceId(undoLogDeleteRequest.getResourceId())
            .build();

        return undoLogDeleteRequestProto;
    }

    @Override
    public UndoLogDeleteRequest convert2Model(UndoLogDeleteRequestProto undoLogDeleteRequestProto) {
        UndoLogDeleteRequest undoLogDeleteRequest = new UndoLogDeleteRequest();
        undoLogDeleteRequest.setSaveDays((short)undoLogDeleteRequestProto.getSaveDays());
        undoLogDeleteRequest.setResourceId(undoLogDeleteRequestProto.getResourceId());
        undoLogDeleteRequest.setBranchType(BranchType.valueOf(undoLogDeleteRequestProto.getBranchType().name()));

        return undoLogDeleteRequest;
    }
}