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
package io.seata.server.raft.msg;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author funkye
 */
public class RaftSyncMsgSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftSyncMsgSerializer.class);

    public static byte[] encode(RaftSyncMsg raftSyncMsg) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            Serializer serializer =
                EnhancedServiceLoader.load(Serializer.class, SerializerType.getByCode(raftSyncMsg.getCodec()).name());
            Optional.ofNullable(raftSyncMsg.getBody()).ifPresent(value -> raftSyncMsg.setBody(
                CompressorFactory.getCompressor(raftSyncMsg.getCompressor()).compress(serializer.serialize(value))));
            oos.writeObject(raftSyncMsg);
            return bos.toByteArray();
        }
    }

    public static RaftSyncMsg decode(byte[] raftSyncMsgByte) {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(raftSyncMsgByte);
            ObjectInputStream ois = new ObjectInputStream(bin)) {
            RaftSyncMsg raftSyncMsg = (RaftSyncMsg)ois.readObject();
            Serializer serializer =
                EnhancedServiceLoader.load(Serializer.class, SerializerType.getByCode(raftSyncMsg.getCodec()).name());
            Optional.ofNullable(raftSyncMsg.getBody())
                .ifPresent(value -> raftSyncMsg.setBody(serializer.deserialize(CompressorFactory
                    .getCompressor(raftSyncMsg.getCompressor()).decompress((byte[])raftSyncMsg.getBody()))));
            return raftSyncMsg;
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.info("Failed to read raft synchronization log: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
