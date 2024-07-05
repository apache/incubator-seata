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
package org.apache.seata.core.protocol;

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;

/**
 * @since 0.7.0
 */
public interface ProtocolConstants {

    /**
     * Magic code
     */
    byte[] MAGIC_CODE_BYTES = {(byte) 0xda, (byte) 0xda};

    /**
     * Old protocol version
     */
    byte VERSION_0 = 0;

    /**
     * Protocol version
     */
    byte VERSION_1 = 1;

    /**
     * Protocol version
     */
    byte VERSION = VERSION_1;

    /**
     * Max frame length
     */
    int MAX_FRAME_LENGTH = 8 * 1024 * 1024;

    /**
     * HEAD_LENGTH of protocol v1
     */
    int V1_HEAD_LENGTH = 16;
    
    /**
     * Message type: Request
     */
    byte MSGTYPE_RESQUEST_SYNC = 0;
    /**
     * Message type: Response
     */
    byte MSGTYPE_RESPONSE = 1;
    /**
     * Message type: Request which no need response
     */
    byte MSGTYPE_RESQUEST_ONEWAY = 2;
    /**
     * Message type: Heartbeat Request
     */
    byte MSGTYPE_HEARTBEAT_REQUEST = 3;
    /**
     * Message type: Heartbeat Response
     */
    byte MSGTYPE_HEARTBEAT_RESPONSE = 4;

    //byte MSGTYPE_NEGOTIATOR_REQUEST = 5;
    //byte MSGTYPE_NEGOTIATOR_RESPONSE = 6;

    /**
     * Configured codec by user, default is SEATA
     * 
     * @see SerializerType#SEATA
     */
    byte CONFIGURED_CODEC = SerializerServiceLoader.getDefaultSerializerType().getCode();

    /**
     * Configured compressor by user, default is NONE
     *
     * @see CompressorType#NONE
     */
    byte CONFIGURED_COMPRESSOR = CompressorType.getByName(ConfigurationFactory.getInstance()
            .getConfig(ConfigurationKeys.COMPRESSOR_FOR_RPC, CompressorType.NONE.name())).getCode();
}
