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
package org.apache.seata.server.cluster.raft.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.compressor.CompressorFactory;
import io.seata.core.serializer.Serializer;
import io.seata.core.serializer.SerializerType;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class RaftSyncMessageSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftSyncMessageSerializer.class);

    public static byte[] encode(RaftSyncMessage raftSyncMessage) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            Serializer serializer =
                EnhancedServiceLoader.load(Serializer.class, SerializerType.getByCode(raftSyncMessage.getCodec()).name());
            Optional.ofNullable(raftSyncMessage.getBody()).ifPresent(value -> raftSyncMessage.setBody(
                CompressorFactory.getCompressor(raftSyncMessage.getCompressor()).compress(serializer.serialize(value))));
            oos.writeObject(raftSyncMessage);
            return bos.toByteArray();
        }
    }

    public static RaftSyncMessage decode(byte[] raftSyncMsgByte) {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(raftSyncMsgByte);
            ObjectInputStream ois = new ObjectInputStream(bin)) {
            RaftSyncMessage raftSyncMessage = (RaftSyncMessage)ois.readObject();
            Serializer serializer =
                EnhancedServiceLoader.load(Serializer.class, SerializerType.getByCode(raftSyncMessage.getCodec()).name());
            Optional.ofNullable(raftSyncMessage.getBody())
                .ifPresent(value -> raftSyncMessage.setBody(serializer.deserialize(CompressorFactory
                    .getCompressor(raftSyncMessage.getCompressor()).decompress((byte[]) raftSyncMessage.getBody()))));
            return raftSyncMessage;
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.info("Failed to read raft synchronization log: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
