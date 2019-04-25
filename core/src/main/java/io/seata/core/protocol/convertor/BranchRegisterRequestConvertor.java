/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.BranchRegisterRequestProto;
import io.seata.core.protocol.transaction.BranchRegisterRequest;

/**
 * @author bystander
 * @version : BranchRegisterRequestConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchRegisterRequestConvertor implements PbConvertor<BranchRegisterRequest, BranchRegisterRequestProto> {
    @Override
    public BranchRegisterRequestProto convert2Proto(BranchRegisterRequest branchRegisterRequest) {
        return null;
    }

    @Override
    public BranchRegisterRequest convert2Model(BranchRegisterRequestProto branchRegisterRequestProto) {
        return null;
    }
}