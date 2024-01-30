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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.apache.seata.common.DefaultValues;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.COMPRESS_PREFIX;


@Component
@ConfigurationProperties(prefix = COMPRESS_PREFIX)
public class UndoCompressProperties {
    private boolean enable = DefaultValues.DEFAULT_CLIENT_UNDO_COMPRESS_ENABLE;
    private String type = DefaultValues.DEFAULT_CLIENT_UNDO_COMPRESS_TYPE;
    private String threshold = DefaultValues.DEFAULT_CLIENT_UNDO_COMPRESS_THRESHOLD;

    public boolean isEnable() {
        return enable;
    }

    public UndoCompressProperties setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public String getType() {
        return type;
    }

    public UndoCompressProperties setType(String type) {
        this.type = type;
        return this;
    }

    public String getThreshold() {
        return threshold;
    }

    public UndoCompressProperties setThreshold(String threshold) {
        this.threshold = threshold;
        return this;
    }
}
