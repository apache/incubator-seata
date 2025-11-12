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
package org.apache.seata.common.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * File loader to load files from file system
 */
public class FileLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileLoader.class);

    /**
     * Load file by name.
     *
     * @param name the file name
     * @return the file if found, or null if not found
     * @throws IllegalArgumentException if name is null
     */
    public static File load(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name can't be null");
        }

        try {
            String decodedPath = URLDecoder.decode(name, StandardCharsets.UTF_8.name());
            return getFileFromFileSystem(decodedPath);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Failed to decode file name: {}", name, e);
        }

        return null;
    }

    /**
     * Get file from file system with multiple path attempts.
     *
     * @param decodedPath the decoded file path
     * @return the file if found, or null if not found
     */
    private static File getFileFromFileSystem(String decodedPath) {
        // run with jar file and not package third lib into jar file, this.getClass().getClassLoader() will be null
        URL resourceUrl = FileLoader.class.getClassLoader().getResource("");
        String[] tryPaths;

        if (resourceUrl != null) {
            tryPaths = new String[] {
                // first: project dir
                resourceUrl.getPath() + decodedPath,
                // second: system path
                decodedPath
            };
        } else {
            tryPaths = new String[] {decodedPath};
        }

        for (String tryPath : tryPaths) {
            File targetFile = new File(tryPath);
            if (targetFile.exists()) {
                return targetFile;
            }
        }

        return null;
    }
}
