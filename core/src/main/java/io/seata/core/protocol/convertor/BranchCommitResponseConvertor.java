/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.BranchCommitResponseProto;
import io.seata.core.protocol.transaction.BranchCommitResponse;

/**
 * @author bystander
 * @version : BranchCommitResponseConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchCommitResponseConvertor implements PbConvertor<BranchCommitResponse, BranchCommitResponseProto> {
    @Override
    public BranchCommitResponseProto convert2Proto(BranchCommitResponse branchCommitResponse) {
        return null;
    }

    @Override
    public BranchCommitResponse convert2Model(BranchCommitResponseProto branchCommitResponseProto) {
        return null;
    }
}