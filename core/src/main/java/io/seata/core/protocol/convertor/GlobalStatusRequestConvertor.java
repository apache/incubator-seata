/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.GlobalStatusRequestProto;
import io.seata.core.protocol.transaction.GlobalStatusRequest;

/**
 * @author bystander
 * @version : GlobalStatusRequestConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalStatusRequestConvertor implements PbConvertor<GlobalStatusRequest, GlobalStatusRequestProto> {
    @Override
    public GlobalStatusRequestProto convert2Proto(GlobalStatusRequest globalStatusRequest) {
        return null;
    }

    @Override
    public GlobalStatusRequest convert2Model(GlobalStatusRequestProto globalStatusRequestProto) {
        return null;
    }
}