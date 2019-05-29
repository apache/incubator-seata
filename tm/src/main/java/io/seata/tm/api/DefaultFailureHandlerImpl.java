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
package io.seata.tm.api;

import java.util.concurrent.TimeUnit;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default failure handler.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /1/8
 */
public class DefaultFailureHandlerImpl implements FailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFailureHandlerImpl.class);

    /**
     * Retry 1 hours by default
     */
    private static final int RETRY_MAX_TIMES = 6 * 60;

    private static final long SCHEDULE_INTERVAL_SECONDS = 10;

    private static final long TICK_DURATION = 1;

    private static final int TICKS_PER_WHEEL = 8;

    private HashedWheelTimer timer = new HashedWheelTimer(
        new NamedThreadFactory("failedTransactionRetry", 1),
        TICK_DURATION, TimeUnit.SECONDS, TICKS_PER_WHEEL);

    @Override
    public void onBeginFailure(GlobalTransaction tx, Throwable cause) {
        LOGGER.warn("Failed to begin transaction. ", cause);
    }

    @Override
    public void onCommitFailure(GlobalTransaction tx, Throwable cause) {
        LOGGER.warn("Failed to commit transaction[" + tx.getXid() + "]", cause);
        timer.newTimeout(new CheckTimerTask(tx, GlobalStatus.Committed), SCHEDULE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void onRollbackFailure(GlobalTransaction tx, Throwable cause) {
        LOGGER.warn("Failed to rollback transaction[" + tx.getXid() + "]", cause);
        timer.newTimeout(new CheckTimerTask(tx, GlobalStatus.Rollbacked), SCHEDULE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    protected class CheckTimerTask implements TimerTask {

        private final GlobalTransaction tx;

        private final GlobalStatus required;

        private int count = 0;

        private boolean isStopped = false;

        protected CheckTimerTask(final GlobalTransaction tx, GlobalStatus required) {
            this.tx = tx;
            this.required = required;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            if (!isStopped) {
                if (++count > RETRY_MAX_TIMES) {
                    LOGGER.error(
                        "transaction[" + tx.getXid() + "] retry fetch status times exceed the limit [" + RETRY_MAX_TIMES
                            + " times]");
                    return;
                }
                isStopped = shouldStop(tx, required);
                timer.newTimeout(this, SCHEDULE_INTERVAL_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    private boolean shouldStop(final GlobalTransaction tx, GlobalStatus required) {
        try {
            GlobalStatus status = tx.getStatus();
            LOGGER.info("transaction[" + tx.getXid() + "] current status is [" + status + "]");
            if (status == required || status == GlobalStatus.Finished) {
                return true;
            }
        } catch (TransactionException e) {
            LOGGER.error("fetch GlobalTransaction status error", e);
        }
        return false;
    }

}
