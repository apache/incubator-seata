/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm;

import com.alibaba.fescar.core.rpc.netty.RmMessageListener;
import com.alibaba.fescar.core.rpc.netty.RmRpcClient;
import com.alibaba.fescar.rm.datasource.AsyncWorker;
import com.alibaba.fescar.rm.datasource.DataSourceManager;

public class RMClientAT {

    public static void init(String applicationId, String transactionServiceGroup) {
        RmRpcClient rmRpcClient = RmRpcClient.getInstance(applicationId, transactionServiceGroup);
        AsyncWorker asyncWorker = new AsyncWorker();
        asyncWorker.init();
        DataSourceManager.init(asyncWorker);
        rmRpcClient.setResourceManager(DataSourceManager.get());
        rmRpcClient.setClientMessageListener(new RmMessageListener(new RMHandlerAT()));
        rmRpcClient.init();
    }
}
