/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.integration.brpc.server.impl;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.integration.brpc.dto.Echo;
import io.seata.integration.brpc.server.EchoService;

import java.util.Objects;

/**
 * @author mxz0828@163.com
 */
public class EchoServiceImpl implements EchoService {
    @Override
    public Echo.EchoResponse echo(Echo.EchoRequest request) {
        Echo.EchoResponse.Builder responseBuilder = Echo.EchoResponse.newBuilder();
        responseBuilder.setReqMsg(request.getReqMsg());
        responseBuilder.setXid(RootContext.getXID());
        BranchType branchType = RootContext.getBranchType();
        if (Objects.nonNull(branchType)) {
            responseBuilder.setBranchType(branchType.name());
        }
        return responseBuilder.build();
    }

}
