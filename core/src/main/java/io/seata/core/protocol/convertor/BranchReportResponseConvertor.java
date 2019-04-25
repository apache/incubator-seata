/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.BranchReportResponseProto;
import io.seata.core.protocol.transaction.BranchReportResponse;

/**
 * @author bystander
 * @version : BranchReportResponseConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchReportResponseConvertor implements PbConvertor<BranchReportResponse, BranchReportResponseProto> {
    @Override
    public BranchReportResponseProto convert2Proto(BranchReportResponse branchReportResponse) {
        return null;
    }

    @Override
    public BranchReportResponse convert2Model(BranchReportResponseProto branchReportResponseProto) {
        return null;
    }
}