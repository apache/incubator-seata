/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.protobuf.RegisterRMRequestProto;

/**
 * @author bystander
 * @version : RegisterRMRequestConvertor.java, v 0.1 2019年04月25日 08:51 bystander Exp $
 */
public class RegisterRMRequestConvertor implements PbConvertor<RegisterRMRequest, RegisterRMRequestProto> {
    @Override
    public RegisterRMRequestProto convert2Proto(RegisterRMRequest registerRMRequest) {
        return null;
    }

    @Override
    public RegisterRMRequest convert2Model(RegisterRMRequestProto registerRMRequestProto) {
        return null;
    }
}