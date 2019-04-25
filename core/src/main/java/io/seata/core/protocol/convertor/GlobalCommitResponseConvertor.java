/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.protobuf.GlobalCommitResponseProto;

/**
 * @author bystander
 * @version : GlobalCommitResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalCommitResponseConvertor implements PbConvertor<GlobalCommitResponse, GlobalCommitResponseProto> {
    @Override
    public GlobalCommitResponseProto convert2Proto(GlobalCommitResponse globalCommitResponse) {
        return null;
    }

    @Override
    public GlobalCommitResponse convert2Model(GlobalCommitResponseProto globalCommitResponseProto) {
        return null;
    }
}