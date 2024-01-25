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
package org.apache.seata.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.env.ContainerHelper;
import org.apache.seata.server.store.StoreConfig;

import static org.apache.seata.config.ConfigurationFactory.ENV_PROPERTY_KEY;

/**
 * The type Parameter parser.
 *
 */
public class ParameterParser {

    private static final String PROGRAM_NAME
        = "sh seata-server.sh(for linux and mac) or cmd seata-server.bat(for windows)";

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Parameter(names = "--help", help = true)
    private boolean help;
    @Parameter(names = {"--host", "-h"}, description = "The ip to register to registry center.", order = 1)
    private String host;
    @Parameter(names = {"--port", "-p"}, description = "The port to listen.", order = 2)
    private int port;
    @Parameter(names = {"--storeMode", "-m"}, description = "log store mode : file, db, redis", order = 3)
    private String storeMode;
    @Parameter(names = {"--serverNode", "-n"}, description = "server node id, such as 1, 2, 3.it will be generated according to the snowflake by default", order = 4)
    private Long serverNode;
    @Parameter(names = {"--seataEnv", "-e"}, description = "The name used for multi-configuration isolation.",
        order = 5)
    private String seataEnv;
    @Parameter(names = {"--sessionStoreMode", "-ssm"}, description = "session log store mode : file, db, redis",
        order = 6)
    private String sessionStoreMode;
    @Parameter(names = {"--lockStoreMode", "-lsm"}, description = "lock log store mode : file, db, redis", order = 7)
    private String lockStoreMode;

    /**
     * Instantiates a new Parameter parser.
     *
     * @param args the args
     */
    public ParameterParser(String... args) {
        this.init(args);
    }

    /**
     * startup args > docker env
     * @param args
     */
    private void init(String[] args) {
        try {
            getCommandParameters(args);
            getEnvParameters();
            if (StringUtils.isNotBlank(seataEnv)) {
                System.setProperty(ENV_PROPERTY_KEY, seataEnv);
            }
            StoreConfig.setStartupParameter(storeMode, sessionStoreMode, lockStoreMode);
        } catch (ParameterException e) {
            printError(e);
        }

    }

    private void getCommandParameters(String[] args) {
        JCommander jCommander = JCommander.newBuilder().addObject(this).build();
        jCommander.parse(args);
        if (help) {
            jCommander.setProgramName(PROGRAM_NAME);
            jCommander.usage();
            System.exit(0);
        }
    }

    private void getEnvParameters() {
        if (StringUtils.isBlank(seataEnv)) {
            seataEnv = ContainerHelper.getEnv();
        }
        if (StringUtils.isBlank(host)) {
            host = ContainerHelper.getHost();
        }
        if (port == 0) {
            port = ContainerHelper.getPort();
        }
        if (serverNode == null) {
            serverNode = ContainerHelper.getServerNode();
        }
    }

    private void printError(ParameterException e) {
        System.err.println("Option error " + e.getMessage());
        e.getJCommander().setProgramName(PROGRAM_NAME);
        e.usage();
        System.exit(0);
    }

    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets store mode.
     *
     * @return the store mode
     */
    public String getStoreMode() {
        return storeMode;
    }

    /**
     * Gets lock store mode.
     *
     * @return the store mode
     */
    public String getLockStoreMode() {
        return lockStoreMode;
    }

    /**
     * Gets session store mode.
     *
     * @return the store mode
     */
    public String getSessionStoreMode() {
        return sessionStoreMode;
    }

    /**
     * Is help boolean.
     *
     * @return the boolean
     */
    public boolean isHelp() {
        return help;
    }

    /**
     * Gets server node.
     *
     * @return the server node
     */
    public Long getServerNode() {
        return serverNode;
    }

    /**
     * Gets seata env
     *
     * @return the name used for multi-configuration isolation.
     */
    public String getSeataEnv() {
        return seataEnv;
    }

    /**
     * Clean up.
     */
    public void cleanUp() {
        if (null != System.getProperty(ENV_PROPERTY_KEY)) {
            System.clearProperty(ENV_PROPERTY_KEY);
        }
    }

}
