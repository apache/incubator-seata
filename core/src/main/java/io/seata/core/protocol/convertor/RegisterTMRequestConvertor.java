/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.protobuf.RegisterTMRequestProto;

/**
 * @author bystander
 * @version : RegisterTMRequestConvertor.java, v 0.1 2019年04月25日 08:51 bystander Exp $
 */
public class RegisterTMRequestConvertor implements PbConvertor<RegisterTMRequest, RegisterTMRequestProto> {
    @Override
    public RegisterTMRequestProto convert2Proto(RegisterTMRequest registerTMRequest) {
        return null;
    }

    @Override
    public RegisterTMRequest convert2Model(RegisterTMRequestProto registerTMRequestProto) {
        return null;
    }
}