/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.protobuf.GlobalLockQueryResponseProto;

/**
 * @author bystander
 * @version : GlobalLockQueryResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalLockQueryResponseConvertor implements PbConvertor<GlobalLockQueryResponse, GlobalLockQueryResponseProto> {
    @Override
    public GlobalLockQueryResponseProto convert2Proto(GlobalLockQueryResponse globalLockQueryResponse) {
        return null;
    }

    @Override
    public GlobalLockQueryResponse convert2Model(GlobalLockQueryResponseProto globalLockQueryResponseProto) {
        return null;
    }
}