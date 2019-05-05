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
package io.seata.core.protocol.serialize;

/**
 * The enum serialize type.
 *
 * @author leizhiyuan
 */
public enum SerializeType {

    /**
     * The seata.
     */
    // AT Branch
    SEATA("seata"),

    /**
     * The protobuf.
     */
    PROTOBUF("protobuf");

    private final String code;

    private SerializeType(final String code) {
        this.code = code;
    }

    public static SerializeType getResultCode(String code) {
        if (code != null) {
            for (SerializeType b : SerializeType.values()) {
                if (code.equalsIgnoreCase(b.code)) {
                    return b;
                }
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }
}
