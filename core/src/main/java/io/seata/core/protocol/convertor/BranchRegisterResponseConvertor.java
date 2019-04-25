/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.BranchRegisterResponseProto;
import io.seata.core.protocol.transaction.BranchRegisterResponse;

/**
 * @author bystander
 * @version : BranchRegisterResponseConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchRegisterResponseConvertor implements PbConvertor<BranchRegisterResponse, BranchRegisterResponseProto> {
    @Override
    public BranchRegisterResponseProto convert2Proto(BranchRegisterResponse branchRegisterResponse) {
        return null;
    }

    @Override
    public BranchRegisterResponse convert2Model(BranchRegisterResponseProto branchRegisterResponseProto) {
        return null;
    }
}