/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.BranchCommitRequestProto;
import io.seata.core.protocol.transaction.BranchCommitRequest;

/**
 * @author bystander
 * @version : BranchCommitRequestConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchCommitRequestConvertor implements PbConvertor<BranchCommitRequest, BranchCommitRequestProto> {
    @Override
    public BranchCommitRequestProto convert2Proto(BranchCommitRequest branchCommitRequest) {
        return null;
    }

    @Override
    public BranchCommitRequest convert2Model(BranchCommitRequestProto branchCommitRequestProto) {
        return null;
    }
}