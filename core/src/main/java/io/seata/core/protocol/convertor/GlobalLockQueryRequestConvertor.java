/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.GlobalLockQueryRequestProto;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;

/**
 * @author bystander
 * @version : GlobalLockQueryRequestConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalLockQueryRequestConvertor implements PbConvertor<GlobalLockQueryRequest, GlobalLockQueryRequestProto> {
    @Override
    public GlobalLockQueryRequestProto convert2Proto(GlobalLockQueryRequest globalLockQueryRequest) {
        return null;
    }

    @Override
    public GlobalLockQueryRequest convert2Model(GlobalLockQueryRequestProto globalLockQueryRequestProto) {
        return null;
    }
}