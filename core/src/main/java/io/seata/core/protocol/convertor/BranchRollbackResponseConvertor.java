/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.BranchRollbackResponseProto;
import io.seata.core.protocol.transaction.BranchRollbackResponse;

/**
 * @author bystander
 * @version : BranchRollbackResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class BranchRollbackResponseConvertor implements PbConvertor<BranchRollbackResponse, BranchRollbackResponseProto> {
    @Override
    public BranchRollbackResponseProto convert2Proto(BranchRollbackResponse branchRollbackResponse) {
        return null;
    }

    @Override
    public BranchRollbackResponse convert2Model(BranchRollbackResponseProto branchRollbackResponseProto) {
        return null;
    }
}