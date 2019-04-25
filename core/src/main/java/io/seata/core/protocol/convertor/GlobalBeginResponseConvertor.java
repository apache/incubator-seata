/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.GlobalBeginResponseProto;
import io.seata.core.protocol.transaction.GlobalBeginResponse;

/**
 * @author bystander
 * @version : GlobalBeginResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalBeginResponseConvertor implements PbConvertor<GlobalBeginResponse, GlobalBeginResponseProto> {
    @Override
    public GlobalBeginResponseProto convert2Proto(GlobalBeginResponse globalBeginResponse) {
        return null;
    }

    @Override
    public GlobalBeginResponse convert2Model(GlobalBeginResponseProto globalBeginResponseProto) {
        return null;
    }
}