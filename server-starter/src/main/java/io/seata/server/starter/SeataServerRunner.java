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
package io.seata.server.starter;

import io.seata.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author spilledyear@outlook.com
 */
@Component
public class SeataServerRunner implements CommandLineRunner {

    final Logger logger = LoggerFactory.getLogger(SeataServerRunner.class);

    private Boolean started = Boolean.FALSE;


    @Override
    public void run(String... args) {
        try {
            synchronized (started) {
                long start = System.currentTimeMillis();
                Server.main(args);
                long cost = System.currentTimeMillis() - start;

                started = true;
                logger.info("seata server started in {} millSeconds", cost);
            }
        } catch (Throwable e) {
            logger.error("seata server start error: {} ", e.getMessage(), e);
            System.exit(-1);
        }
    }


    public boolean started() {
        return started;
    }
}
