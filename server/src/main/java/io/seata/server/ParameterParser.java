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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;

import static io.seata.config.ConfigurationFactory.ENV_PROPERTY_KEY;

/**
 * The type Parameter parser.
 *
 * @author xingfudeshi@gmail.com
 * @date 2019/05/30
 */
public class ParameterParser {
    private static final String PROGRAM_NAME = "sh seata-server.sh(for linux and mac) or cmd seata-server.bat(for windows)";
    private static final int SERVER_DEFAULT_PORT = 8091;
    private static final String SERVER_DEFAULT_STORE_MODE = "file";
    private static final int SERVER_DEFAULT_NODE = 1;

    @Parameter(names = "--help", help = true)
    private boolean help;
    @Parameter(names = {"--host", "-h"}, description = "The ip to register to registry center.", order = 1)
    private String host;
    @Parameter(names = {"--port", "-p"}, description = "The port to listen.", order = 2)
    private int port = SERVER_DEFAULT_PORT;
    @Parameter(names = {"--storeMode", "-m"}, description = "log store mode : file, db", order = 3)
    private String storeMode;
    @Parameter(names = {"--serverNode", "-n"}, description = "server node id, such as 1, 2, 3. default is 1", order = 4)
    private int serverNode = SERVER_DEFAULT_NODE;
    @Parameter(names = {"--seataEnv", "-e"}, description = "The name used for multi-configuration isolation.", order = 5)
    private String seataEnv;

    /**
     * Instantiates a new Parameter parser.
     *
     * @param args the args
     */
    public ParameterParser(String[] args) {
        this.init(args);
    }

    private void init(String[] args) {
        try {
            JCommander jCommander = JCommander.newBuilder().addObject(this).build();
            jCommander.parse(args);
            if (help) {
                jCommander.setProgramName(PROGRAM_NAME);
                jCommander.usage();
                System.exit(0);
            }
            if (StringUtils.isNotBlank(seataEnv)) {
                System.setProperty(ENV_PROPERTY_KEY, seataEnv);
            }
            if(StringUtils.isBlank(storeMode)){
                storeMode= ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_MODE, SERVER_DEFAULT_STORE_MODE);
            }
        } catch (ParameterException e) {
            printError(e);
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
    public int getServerNode() {
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
}
