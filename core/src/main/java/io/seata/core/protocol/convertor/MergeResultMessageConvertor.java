/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.protobuf.MergedResultMessageProto;

/**
 * @author bystander
 * @version : MergeResultMessageConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class MergeResultMessageConvertor implements PbConvertor<MergeResultMessage, MergedResultMessageProto> {
    @Override
    public MergedResultMessageProto convert2Proto(MergeResultMessage mergeResultMessage) {
        return null;
    }

    @Override
    public MergeResultMessage convert2Model(MergedResultMessageProto mergedResultMessageProto) {
        return null;
    }
}