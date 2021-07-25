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
package io.seata.common.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * file loader
 *
 * @author tianyu.li
 */
public class FileLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileLoader.class);

    public static File load(String name) {
        try {
            if (name == null) {
                throw new IllegalArgumentException("name can't be null");
            }
            String decodedPath = URLDecoder.decode(name, StandardCharsets.UTF_8.name());

            return getFileFromFileSystem(decodedPath);

        } catch (UnsupportedEncodingException e) {
            LOGGER.error("decode name error: {}", e.getMessage(), e);
        }

        return null;
    }

    private static File getFileFromFileSystem(String decodedPath) {

        // run with jar file and not package third lib into jar file, this.getClass().getClassLoader() will be null
        URL resourceUrl = FileLoader.class.getClassLoader().getResource("");
        String[] tryPaths;
        if (resourceUrl != null) {
            tryPaths = new String[]{
                // first: project dir
                resourceUrl.getPath() + decodedPath,
                // second: system path
                decodedPath
            };
        } else {
            tryPaths = new String[]{
                decodedPath
            };
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
