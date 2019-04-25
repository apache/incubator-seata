/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.BranchRollbackRequestProto;
import io.seata.core.protocol.transaction.BranchRollbackRequest;

/**
 * @author bystander
 * @version : BranchRollbackRequestConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchRollbackRequestConvertor implements PbConvertor<BranchRollbackRequest, BranchRollbackRequestProto> {
    @Override
    public BranchRollbackRequestProto convert2Proto(BranchRollbackRequest branchRollbackRequest) {
        return null;
    }

    @Override
    public BranchRollbackRequest convert2Model(BranchRollbackRequestProto branchRollbackRequestProto) {
        return null;
    }
}