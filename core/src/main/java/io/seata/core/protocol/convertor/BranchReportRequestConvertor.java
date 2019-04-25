/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.protobuf.BranchReportRequestProto;

/**
 * @author bystander
 * @version : BranchReportRequestConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchReportRequestConvertor implements PbConvertor<BranchReportRequest, BranchReportRequestProto> {
    @Override
    public BranchReportRequestProto convert2Proto(BranchReportRequest branchReportRequest) {
        return null;
    }

    @Override
    public BranchReportRequest convert2Model(BranchReportRequestProto branchReportRequestProto) {
        return null;
    }
}