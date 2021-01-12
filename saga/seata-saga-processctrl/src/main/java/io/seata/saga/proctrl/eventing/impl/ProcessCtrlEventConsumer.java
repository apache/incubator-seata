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
package io.seata.saga.proctrl.eventing.impl;

import io.seata.common.exception.FrameworkException;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.ProcessController;
import io.seata.saga.proctrl.eventing.EventConsumer;

/**
 * ProcessCtrl Event Consumer
 *
 * @author lorne.cl
 */
public class ProcessCtrlEventConsumer implements EventConsumer<ProcessContext> {

    private ProcessController processController;

    @Override
    public void process(ProcessContext event) throws FrameworkException {

        processController.process(event);
    }

    @Override
    public boolean accept(Class<ProcessContext> clazz) {
        return ProcessContext.class.isAssignableFrom(clazz);
    }

    public void setProcessController(ProcessController processController) {
        this.processController = processController;
    }
}