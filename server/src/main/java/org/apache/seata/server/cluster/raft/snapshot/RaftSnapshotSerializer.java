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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.compressor.CompressorFactory;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerType;

/**
 */
public class RaftSnapshotSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftSnapshotSerializer.class);

    public static byte[] encode(RaftSnapshot raftSnapshot) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            Serializer serializer =
                    EnhancedServiceLoader.load(Serializer.class, SerializerType.getByCode(raftSnapshot.getCodec()).name());
            Optional.ofNullable(raftSnapshot.getBody()).ifPresent(value -> raftSnapshot.setBody(
                    CompressorFactory.getCompressor(raftSnapshot.getCompressor()).compress(serializer.serialize(value))));
            oos.writeObject(raftSnapshot);
            return bos.toByteArray();
        }
    }

    public static byte[] encode(io.seata.server.cluster.raft.snapshot.RaftSnapshot raftSnapshot) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            Serializer serializer =
                    EnhancedServiceLoader.load(Serializer.class, SerializerType.getByCode(raftSnapshot.getCodec()).name());
            Optional.ofNullable(raftSnapshot.getBody()).ifPresent(value -> raftSnapshot.setBody(
                    CompressorFactory.getCompressor(raftSnapshot.getCompressor()).compress(serializer.serialize(value))));
            oos.writeObject(raftSnapshot);
            return bos.toByteArray();
        }
    }

    public static RaftSnapshot decode(byte[] raftSnapshotByte) throws IOException {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(raftSnapshotByte);
                ObjectInputStream ois = new ObjectInputStream(bin)) {
            Object object = ois.readObject();
            RaftSnapshot raftSnapshot;
            if (object instanceof io.seata.server.cluster.raft.snapshot.RaftSnapshot) {
                raftSnapshot = new RaftSnapshot();
                io.seata.server.cluster.raft.snapshot.RaftSnapshot oldRaftSnapshot =
                        (io.seata.server.cluster.raft.snapshot.RaftSnapshot)object;
                raftSnapshot.setBody(oldRaftSnapshot.getBody());
                raftSnapshot.setVersion(oldRaftSnapshot.getVersion());
                raftSnapshot.setCompressor(oldRaftSnapshot.getCompressor());
                raftSnapshot.setType(RaftSnapshot.SnapshotType.valueOf(oldRaftSnapshot.getType().name()));
            } else {
                raftSnapshot = (RaftSnapshot)object;
            }
            Serializer serializer =
                    EnhancedServiceLoader.load(Serializer.class, SerializerType.getByCode(raftSnapshot.getCodec()).name());
            Optional.ofNullable(raftSnapshot.getBody())
                    .ifPresent(value -> raftSnapshot.setBody(serializer.deserialize(CompressorFactory
                            .getCompressor(raftSnapshot.getCompressor()).decompress((byte[])raftSnapshot.getBody()))));
            return raftSnapshot;
        } catch (ClassNotFoundException e) {
            LOGGER.info("Failed to read raft snapshot: {}", e.getMessage(), e);
            throw new IOException(e);
        }
    }

}
