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
package org.apache.seata.config.processor;

import org.apache.seata.common.loader.LoadLevel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * The properties Processor.
 *
 */
@LoadLevel(name = "properties")
public class ProcessorProperties implements Processor {

    @Override
    public Properties processor(String config) throws IOException {
        Properties properties = new Properties();

        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(config.getBytes()), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
