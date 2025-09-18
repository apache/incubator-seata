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
package org.apache.seata.serializer.seata;

import org.apache.seata.core.protocol.IncompatibleVersionException;
import org.apache.seata.core.protocol.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * the type MultiVersionCodecHelper
 *
 **/
public class MultiVersionCodecHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiVersionCodecHelper.class);

    public static MessageSeataCodec match(String v, MessageSeataCodec messageCodec) {
        try {
            if (!(messageCodec instanceof MultiVersionCodec)) {
                return null;
            }
            Map<MultiVersionCodec.VersionRange, MessageSeataCodec> map =
                    ((MultiVersionCodec) messageCodec).oldVersionCodec();
            long version = Version.convertVersion(v);
            for (MultiVersionCodec.VersionRange range : map.keySet()) {
                if (version > Version.convertVersion(range.getBegin())
                        && version <= Version.convertVersion(range.getEnd())) {
                    return map.get(version);
                }
            }
        } catch (IncompatibleVersionException e) {
            LOGGER.error("match version error", e);
        }
        return null;
    }
}
