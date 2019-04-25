/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.GlobalRollbackRequestProto;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;

/**
 * @author bystander
 * @version : GlobalRollbackRequestConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalRollbackRequestConvertor implements PbConvertor<GlobalRollbackRequest, GlobalRollbackRequestProto> {
    @Override
    public GlobalRollbackRequestProto convert2Proto(GlobalRollbackRequest globalRollbackRequest) {
        return null;
    }

    @Override
    public GlobalRollbackRequest convert2Model(GlobalRollbackRequestProto globalRollbackRequestProto) {
        return null;
    }
}