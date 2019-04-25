/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.GlobalCommitRequestProto;
import io.seata.core.protocol.transaction.GlobalCommitRequest;

/**
 * @author bystander
 * @version : GlobalCommitRequestConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalCommitRequestConvertor implements PbConvertor<GlobalCommitRequest, GlobalCommitRequestProto> {
    @Override
    public GlobalCommitRequestProto convert2Proto(GlobalCommitRequest globalCommitRequest) {
        return null;
    }

    @Override
    public GlobalCommitRequest convert2Model(GlobalCommitRequestProto globalCommitRequestProto) {
        return null;
    }
}