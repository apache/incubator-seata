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
package io.seata.core.rpc.netty.v0;

/**
 * protocol v0 constants
 *
 * @author Bughue
 * @date 2023/7/18
 **/
public class ProtocolConstantsV0 {
    public static short MAGIC = (short)0xdada;

    public static int HEAD_LENGTH = 14;
    public static final short FLAG_REQUEST = 0x80;
    public static final short FLAG_ASYNC = 0x40;
    public static final short FLAG_HEARTBEAT = 0x20;
    public static final short FLAG_SEATA_CODEC = 0x10;
}
