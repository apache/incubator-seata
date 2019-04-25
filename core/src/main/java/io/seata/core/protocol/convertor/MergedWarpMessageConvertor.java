/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.protobuf.MergedWarpMessageProto;

/**
 * @author bystander
 * @version : MergedWarpMessageConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class MergedWarpMessageConvertor implements PbConvertor<MergedWarpMessage, MergedWarpMessageProto> {
    @Override
    public MergedWarpMessageProto convert2Proto(MergedWarpMessage mergedWarpMessage) {
        return null;
    }

    @Override
    public MergedWarpMessage convert2Model(MergedWarpMessageProto mergedWarpMessageProto) {
        return null;
    }
}