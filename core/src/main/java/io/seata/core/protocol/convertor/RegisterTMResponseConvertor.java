/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.protobuf.RegisterTMResponseProto;

/**
 * @author bystander
 * @version : RegisterTMResponseConvertor.java, v 0.1 2019年04月25日 08:51 bystander Exp $
 */
public class RegisterTMResponseConvertor implements PbConvertor<RegisterTMResponse, RegisterTMResponseProto> {
    @Override
    public RegisterTMResponseProto convert2Proto(RegisterTMResponse registerTMResponse) {
        return null;
    }

    @Override
    public RegisterTMResponse convert2Model(RegisterTMResponseProto registerTMResponseProto) {
        return null;
    }
}