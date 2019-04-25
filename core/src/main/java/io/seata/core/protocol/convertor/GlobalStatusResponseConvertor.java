/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.GlobalStatusResponseProto;
import io.seata.core.protocol.transaction.GlobalStatusResponse;

/**
 * @author bystander
 * @version : GlobalStatusResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalStatusResponseConvertor implements PbConvertor<GlobalStatusResponse, GlobalStatusResponseProto> {
    @Override
    public GlobalStatusResponseProto convert2Proto(GlobalStatusResponse globalStatusResponse) {
        return null;
    }

    @Override
    public GlobalStatusResponse convert2Model(GlobalStatusResponseProto globalStatusResponseProto) {
        return null;
    }
}