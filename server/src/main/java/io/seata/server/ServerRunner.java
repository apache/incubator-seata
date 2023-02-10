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
package io.seata.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.seata.core.rpc.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * @author spilledyear@outlook.com
 */
@Component
public class ServerRunner implements CommandLineRunner, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRunner.class);

    private boolean started = Boolean.FALSE;

    private static final List<Disposable> DISPOSABLE_LIST = new CopyOnWriteArrayList<>();

    public static void addDisposable(Disposable disposable) {
        DISPOSABLE_LIST.add(disposable);
    }

    @Override
    public void run(String... args) {
        try {
            long start = System.currentTimeMillis();
            Server.start(args);
            started = true;

            long cost = System.currentTimeMillis() - start;
            LOGGER.info("seata server started in {} millSeconds", cost);
        } catch (Throwable e) {
            started = Boolean.FALSE;
            LOGGER.error("seata server start error: {} ", e.getMessage(), e);
            System.exit(-1);
        }
    }


    public boolean started() {
        return started;
    }

    @Override
    public void destroy() throws Exception {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("destoryAll starting");
        }

        for (Disposable disposable : DISPOSABLE_LIST) {
            disposable.destroy();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("destoryAll finish");
        }
    }
}
