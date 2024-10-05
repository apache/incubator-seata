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
package org.apache.seata.core.rpc.netty.grpc;

public enum GrpcHeaderEnum {

    /**
     * grpc status
     */
    GRPC_STATUS("grpc-status"),
    /**
     * http2 status
     */
    HTTP2_STATUS(":status"),
    /**
     * content-type
     */
    GRPC_CONTENT_TYPE("content-type"),

    /**
     * codec-type
     */
    CODEC_TYPE("codec-type"),

    /**
     * compress-type
     */
    COMPRESS_TYPE("compress-type");

    public final String header;

    GrpcHeaderEnum(String header) {
        this.header = header;
    }
}
