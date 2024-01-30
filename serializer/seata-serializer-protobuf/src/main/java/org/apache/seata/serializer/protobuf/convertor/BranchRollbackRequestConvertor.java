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

import org.apache.seata.core.model.BranchType;
import org.apache.seata.serializer.protobuf.generated.AbstractBranchEndRequestProto;
import org.apache.seata.serializer.protobuf.generated.AbstractMessageProto;
import org.apache.seata.serializer.protobuf.generated.AbstractTransactionRequestProto;
import org.apache.seata.serializer.protobuf.generated.BranchRollbackRequestProto;
import org.apache.seata.serializer.protobuf.generated.BranchTypeProto;
import org.apache.seata.serializer.protobuf.generated.MessageTypeProto;
import org.apache.seata.core.protocol.transaction.BranchRollbackRequest;


public class BranchRollbackRequestConvertor implements PbConvertor<BranchRollbackRequest, BranchRollbackRequestProto> {
    @Override
    public BranchRollbackRequestProto convert2Proto(BranchRollbackRequest branchRollbackRequest) {
        final short typeCode = branchRollbackRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(abstractMessage).build();

        final String applicationData = branchRollbackRequest.getApplicationData();
        final String resourceId = branchRollbackRequest.getResourceId();
        final AbstractBranchEndRequestProto abstractBranchEndRequestProto = AbstractBranchEndRequestProto.newBuilder().
            setAbstractTransactionRequest(abstractTransactionRequestProto).setXid(branchRollbackRequest.getXid())
            .setBranchId(branchRollbackRequest.getBranchId()).setBranchType(
                BranchTypeProto.valueOf(branchRollbackRequest.getBranchType().name())).setApplicationData(
                applicationData == null ? "" : applicationData).setResourceId(resourceId == null ? "" : resourceId)
            .build();

        BranchRollbackRequestProto result = BranchRollbackRequestProto.newBuilder().setAbstractBranchEndRequest(
            abstractBranchEndRequestProto).build();

        return result;
    }

    @Override
    public BranchRollbackRequest convert2Model(BranchRollbackRequestProto branchRollbackRequestProto) {
        BranchRollbackRequest branchCommitRequest = new BranchRollbackRequest();
        branchCommitRequest.setApplicationData(
            branchRollbackRequestProto.getAbstractBranchEndRequest().getApplicationData());
        branchCommitRequest.setBranchId(branchRollbackRequestProto.getAbstractBranchEndRequest().getBranchId());
        branchCommitRequest.setResourceId(branchRollbackRequestProto.getAbstractBranchEndRequest().getResourceId());
        branchCommitRequest.setXid(branchRollbackRequestProto.getAbstractBranchEndRequest().getXid());
        branchCommitRequest.setBranchType(
            BranchType.valueOf(branchRollbackRequestProto.getAbstractBranchEndRequest().getBranchType().name()));

        return branchCommitRequest;
    }
}
