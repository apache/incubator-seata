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
package org.apache.seata.saga.proctrl.impl;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.proctrl.ProcessController;
import org.apache.seata.saga.proctrl.process.BusinessProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of Process controller
 *
 */
public class ProcessControllerImpl implements ProcessController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessControllerImpl.class);

    private BusinessProcessor businessProcessor;

    @Override
    public void process(ProcessContext context) throws FrameworkException {

        try {

            businessProcessor.process(context);

            businessProcessor.route(context);

        } catch (FrameworkException fex) {
            throw fex;
        } catch (Exception ex) {
            LOGGER.error("Unknown exception occurred, context = {}", context, ex);
            throw new FrameworkException(ex, "Unknown exception occurred", FrameworkErrorCode.UnknownAppError);
        }
    }

    public void setBusinessProcessor(BusinessProcessor businessProcessor) {
        this.businessProcessor = businessProcessor;
    }
}
