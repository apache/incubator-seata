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
package io.seata.core.protocol;

import io.seata.config.ConfigurationFactory;
import io.seata.core.codec.CodecType;
import io.seata.core.constants.ConfigurationKeys;

/**
 * @author Geng Zhang
 * @since 0.7.0
 */
public class ProtocolConstants {

    public static final byte[] MAGIC_CODE_BYTES = {(byte) 0xda, (byte) 0xda};

    public static final byte VERSION = 1;

    public static final byte MSGTYPE_RESQUEST = 0;
    public static final byte MSGTYPE_RESPONSE = 1;
    public static final byte MSGTYPE_RESQUEST_ONEWAY = 2;
    public static final byte MSGTYPE_HEARTBEAT = 3;

    // public static final byte MSGTYPE_NEGOTIATOR_REQUEST = 4;
    // public static final byte MSGTYPE_NEGOTIATOR_RESPONSE = 5;

    // TODO add compressor like CodecFactory
    public static final byte COMPRESS_NONE = 0;

    public static final byte DEFAULT_CODEC = CodecType.valueOf(ConfigurationFactory.getInstance().getConfig(
            ConfigurationKeys.SERIALIZE_FOR_RPC, CodecType.SEATA.name()).toUpperCase()).getCode();

    public static final byte DEFAULT_COMPRESSOR = COMPRESS_NONE;

}
