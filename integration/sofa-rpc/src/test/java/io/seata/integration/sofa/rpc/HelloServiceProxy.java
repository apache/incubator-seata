/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.integration.sofa.rpc;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;

/**
 * @author Geng Zhang
 */
public class HelloServiceProxy implements HelloService {

    private String xid;

    private BranchType branchType;

    private HelloService proxy;

    public HelloServiceProxy(HelloService proxy) {
        this.proxy = proxy;
    }

    @Override
    public String sayHello(String name, int age) {
        xid = RootContext.getXID();
        branchType = RootContext.getBranchType();
        return proxy.sayHello(name, age);
    }

    public String getXid() {
        return xid;
    }

    public BranchType getBranchType() {
        return branchType;
    }
}
