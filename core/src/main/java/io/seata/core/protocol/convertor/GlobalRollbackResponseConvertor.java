/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.GlobalRollbackResponseProto;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;

/**
 * @author bystander
 * @version : GlobalRollbackResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalRollbackResponseConvertor implements PbConvertor<GlobalRollbackResponse, GlobalRollbackResponseProto> {
    @Override
    public GlobalRollbackResponseProto convert2Proto(GlobalRollbackResponse globalRollbackResponse) {
        return null;
    }

    @Override
    public GlobalRollbackResponse convert2Model(GlobalRollbackResponseProto globalRollbackResponseProto) {
        return null;
    }
}