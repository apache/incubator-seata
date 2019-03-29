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

package com.alibaba.fescar.core.rpc.netty;

import com.alibaba.fescar.common.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ensure the shutdownHook is singleton
 *
 * @author 563868273@qq.com
 * @date 2019/3/29
 */
public class FescarShutdownHook extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(FescarShutdownHook.class);

    private static final FescarShutdownHook FESCAR_SHUTDOWN_HOOK = new FescarShutdownHook("FescarShutdownHook");

    private Set<AbstractRpcRemoting> abstractRpcRemotings = new HashSet<>();

    private final AtomicBoolean destroyed= new AtomicBoolean(false);

    static {
        Runtime.getRuntime().addShutdownHook(FESCAR_SHUTDOWN_HOOK);
    }

    public FescarShutdownHook(String name) {
        super(name);
    }

    public static FescarShutdownHook getInstance(){
        return FESCAR_SHUTDOWN_HOOK;
    }

    public void addAbstractRpcRemoting(AbstractRpcRemoting abstractRpcRemoting){
        abstractRpcRemotings.add(abstractRpcRemoting);
    }

    @Override
    public void run() {
        destroyAll();
    }

    public void destroyAll() {
        if (!destroyed.compareAndSet(false, true) && CollectionUtils.isEmpty(abstractRpcRemotings)){
            return;
        }
        for (AbstractRpcRemoting abstractRpcRemoting : abstractRpcRemotings) {
            abstractRpcRemoting.destroy();
        }
    }

    /**
     * for spring context
     */
    public static void removeRuntimeShutdownHook(){
        Runtime.getRuntime().removeShutdownHook(FESCAR_SHUTDOWN_HOOK);
    }

}
