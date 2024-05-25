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
package org.apache.seata.benchmark.profiler.integration;

import org.apache.seata.benchmark.profiler.model.TccAction;
import org.apache.seata.benchmark.profiler.model.TccActionImpl;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.tcc.TCCResourceManager;


public class InterfaceParserMethods {

    public void tccProxyInvocationHandler() throws Exception {
        TccActionImpl target = new TccActionImpl();
        DefaultInterfaceParser.get().parserInterfaceToProxy(target, TccAction.class.getName());

        //clear resource, prevent resourceId duplication
        TCCResourceManager tccResourceManager = (TCCResourceManager) DefaultResourceManager.get().getResourceManager(BranchType.TCC);
        tccResourceManager.getManagedResources().clear();
    }
}