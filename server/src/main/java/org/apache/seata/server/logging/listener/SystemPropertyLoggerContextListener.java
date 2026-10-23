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
package org.apache.seata.server.logging.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import org.apache.seata.core.constants.ConfigurationKeys;

import java.io.File;
import java.util.Locale;

/**
 */
public class SystemPropertyLoggerContextListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private static final String EXTERNAL_CONF_DIR_PROPERTY = "EXTERNAL_CONF_DIR";

    private static final String EXTERNAL_APPENDER_SUFFIX = "_APPENDER_EXTERNAL";

    private static final String[] EXTERNAL_APPENDER_NAMES = {"console", "file", "logstash", "metric", "kafka"};

    private boolean started = false;

    @Override
    public void start() {
        if (started) {
            return;
        }

        Context context = getContext();
        context.putProperty("RPC_PORT", System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));
        markExternalAppenders(context);

        started = true;
    }

    /**
     * Marks which appender definitions exist in the external configuration directory, so that the logback
     * configuration can prefer the external file and fall back to the classpath copy when it is absent.
     */
    private void markExternalAppenders(Context context) {
        String externalConfDir = context.getProperty(EXTERNAL_CONF_DIR_PROPERTY);
        boolean hasExternalConfDir =
                externalConfDir != null && !externalConfDir.trim().isEmpty();
        for (String appenderName : EXTERNAL_APPENDER_NAMES) {
            boolean externalFileExists = hasExternalConfDir
                    && new File(externalConfDir, "logback/" + appenderName + "-appender.xml").exists();
            context.putProperty(
                    appenderName.toUpperCase(Locale.ROOT) + EXTERNAL_APPENDER_SUFFIX,
                    Boolean.toString(externalFileExists));
        }
    }

    @Override
    public void stop() {}

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onStart(LoggerContext context) {}

    @Override
    public void onReset(LoggerContext context) {}

    @Override
    public void onStop(LoggerContext context) {}

    @Override
    public void onLevelChange(Logger logger, Level level) {}
}
