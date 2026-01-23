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
package org.apache.seata.benchmark.config;

import org.apache.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration loader for benchmark
 */
public class BenchmarkConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkConfigLoader.class);
    private static final String DEFAULT_CONFIG_FILE = "benchmark.yaml";
    private static final String ENV_CONFIG_FILE = "BENCHMARK_CONFIG_FILE";
    private static final String SYS_CONFIG_FILE = "benchmark.config.file";

    public static BenchmarkConfig load(String configFile) throws IOException {
        BenchmarkConfig config;
        String configSource;

        if (configFile != null && !configFile.isEmpty()) {
            config = loadFromFile(configFile);
            configSource = "CLI argument: " + configFile;
        } else {
            String envConfig = System.getenv(ENV_CONFIG_FILE);
            if (envConfig != null && !envConfig.isEmpty()) {
                config = loadFromFile(envConfig);
                configSource = "Environment variable " + ENV_CONFIG_FILE + ": " + envConfig;
            } else {
                String sysConfig = System.getProperty(SYS_CONFIG_FILE);
                if (sysConfig != null && !sysConfig.isEmpty()) {
                    config = loadFromFile(sysConfig);
                    configSource = "System property " + SYS_CONFIG_FILE + ": " + sysConfig;
                } else {
                    config = loadDefault();
                    configSource = "Default classpath: " + DEFAULT_CONFIG_FILE;
                }
            }
        }

        LOGGER.info("Loading configuration from: {}", configSource);
        config.validate();
        return config;
    }

    private static BenchmarkConfig loadFromFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return loadFromStream(fis);
        }
    }

    public static BenchmarkConfig loadDefault() throws IOException {
        try (InputStream is = BenchmarkConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (is == null) {
                LOGGER.warn("Default configuration file not found, using built-in defaults");
                return new BenchmarkConfig();
            }
            return loadFromStream(is);
        }
    }

    private static BenchmarkConfig loadFromStream(InputStream inputStream) {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        return yaml.load(inputStream);
    }

    public static BenchmarkConfig merge(
            BenchmarkConfig config,
            String server,
            String mode,
            Integer targetTps,
            Integer threads,
            Integer duration,
            Integer warmupDuration,
            String applicationId,
            String txServiceGroup,
            Integer rollbackPercentage,
            Integer branches) {
        if (StringUtils.isNotEmpty(server)) {
            config.setServer(server);
        }
        if (StringUtils.isNotEmpty(mode)) {
            config.setMode(mode);
        }
        if (targetTps != null && targetTps > 0) {
            config.setTargetTps(targetTps);
        }
        if (threads != null && threads > 0) {
            config.setThreads(threads);
        }
        if (duration != null && duration > 0) {
            config.setDuration(duration);
        }
        if (warmupDuration != null && warmupDuration >= 0) {
            config.setWarmupDuration(warmupDuration);
        }
        if (StringUtils.isNotEmpty(applicationId)) {
            config.setApplicationId(applicationId);
        }
        if (StringUtils.isNotEmpty(txServiceGroup)) {
            config.setTxServiceGroup(txServiceGroup);
        }
        if (rollbackPercentage != null && rollbackPercentage >= 0 && rollbackPercentage <= 100) {
            config.setRollbackPercentage(rollbackPercentage);
        }
        if (branches != null && branches >= 0) {
            config.setBranches(branches);
        }
        return config;
    }
}
