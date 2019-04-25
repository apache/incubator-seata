/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.protobuf.RegisterRMResponseProto;

/**
 * @author bystander
 * @version : RegisterRMResponseConvertor.java, v 0.1 2019年04月25日 08:51 bystander Exp $
 */
public class RegisterRMResponseConvertor implements PbConvertor<RegisterRMResponse, RegisterRMResponseProto> {
    @Override
    public RegisterRMResponseProto convert2Proto(RegisterRMResponse registerRMResponse) {
        return null;
    }

    @Override
    public RegisterRMResponse convert2Model(RegisterRMResponseProto registerRMResponseProto) {
        return null;
    }
}