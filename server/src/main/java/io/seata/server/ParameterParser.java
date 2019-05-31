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

/**
 * The type parameter parser
 *
 * @author xingfudeshi@gmail.com
 * @date 2019/05/30
 */
public class ParameterParser {
    private static final String PROGRAM_NAME = "sh seata-server.sh(for linux and mac) or cmd seata-server.bat(for windows)";
    private static final int SERVER_DEFAULT_PORT = 8091;
    private static final String SERVER_DEFAULT_BIND_IP = "0.0.0.0";
    private static final String SERVER_DEFAULT_STORE_MODE = "file";


    @Parameter(names = "--help", help = true)
    private boolean help;
    @Parameter(names = {"--host", "-h"}, description = "The host to bind.", order = 1)
    private String host = SERVER_DEFAULT_BIND_IP;
    @Parameter(names = {"--port", "-p"}, description = "The port to listen.", order = 2)
    private int port = SERVER_DEFAULT_PORT;
    @Parameter(names = {"--storeMode", "-m"}, description = "log store mode : file、db", order = 3)
    private String storeMode = SERVER_DEFAULT_STORE_MODE;


    public ParameterParser(String[] args) {
        this.init(args);
    }

    /**
     * initialize the parameter parser
     *
     * @param args
     */
    private void init(String[] args) {
        try {
            JCommander jCommander = JCommander.newBuilder().addObject(this).build();
            jCommander.parse(args);
            if (help) {
                jCommander.setProgramName(PROGRAM_NAME);
                jCommander.usage();
                System.exit(0);
            }
        } catch (ParameterException e) {
            printError(e);
        }

    }

    /**
     * print the error
     *
     * @param e
     */
    private void printError(ParameterException e) {
        System.err.println("Option error " + e.getMessage());
        e.getJCommander().setProgramName(PROGRAM_NAME);
        e.usage();
        System.exit(0);
    }

    /**
     * Gets host
     *
     * @return host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets port
     *
     * @return port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets store mode
     *
     * @return storeMode
     */
    public String getStoreMode() {
        return storeMode;
    }

    /**
     * is help
     *
     * @return help
     */
    public boolean isHelp() {
        return help;
    }
}
