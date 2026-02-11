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

import java.util.Map;

/**
 * interface MultiVersionCodec
 */
public interface MultiVersionCodec {

    Map<VersionRange, MessageSeataCodec> oldVersionCodec();

    /**
     * version range (begin, end]
     */
    class VersionRange {
        private String begin;
        private String end;

        public VersionRange(String begin, String end) {
            this.begin = begin;
            this.end = end;
        }

        public VersionRange(String end) {
            this.begin = "0";
            this.end = end;
        }

        public String getBegin() {
            return begin;
        }

        public String getEnd() {
            return end;
        }
    }
}
