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
package org.apache.seata.server.cluster.raft.snapshot;

import java.io.Serializable;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.core.serializer.SerializerType;

import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_COMPRESSOR;
import static org.apache.seata.common.DefaultValues.DEFAULT_RAFT_COMPRESSOR;
import static org.apache.seata.common.DefaultValues.DEFAULT_RAFT_SERIALIZATION;

/**
 */
public class RaftSnapshot implements Serializable {

    private byte codec = SerializerType.getByName(DEFAULT_RAFT_SERIALIZATION).getCode();

    private byte compressor = CompressorType
        .getByName(ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_COMPRESSOR, DEFAULT_RAFT_COMPRESSOR))
        .getCode();

    private Object body;
    
    private String version = Version.getCurrent();
    
    private SnapshotType type;

    /**
     * Gets body.
     *
     * @return the body
     */
    public Object getBody() {
        return body;
    }

    /**
     * Sets body.
     *
     * @param body the body
     */
    public void setBody(Object body) {
        this.body = body;
    }

    /**
     * Gets codec.
     *
     * @return the codec
     */
    public byte getCodec() {
        return codec;
    }

    /**
     * Sets codec.
     *
     * @param codec the codec
     * @return the codec
     */
    public RaftSnapshot setCodec(byte codec) {
        this.codec = codec;
        return this;
    }

    /**
     * Gets compressor.
     *
     * @return the compressor
     */
    public byte getCompressor() {
        return compressor;
    }

    /**
     * Sets compressor.
     *
     * @param compressor the compressor
     * @return the compressor
     */
    public RaftSnapshot setCompressor(byte compressor) {
        this.compressor = compressor;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public SnapshotType getType() {
        return type;
    }

    public void setType(SnapshotType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

    public enum SnapshotType {

        /**
         * session snapshot
         */
        session("session"),
        /**
         * leader metadata snapshot
         */
        leader_metadata("leader_metadata");

        final String type;

        SnapshotType(String type) {
            this.type = type;
        }

    }
    
}
