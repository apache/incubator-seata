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

import java.io.File;
import java.io.IOException;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.commons.io.FileUtils;

/**
 */
public interface StoreSnapshotFile {

    /**
     * Save a snapshot .
     *
     * @param writer snapshot writer
     * @return true if save succeed
     */
    Status save(final SnapshotWriter writer);

    /**
     * Load snapshot for the specified region.
     *
     * @param reader snapshot reader
     * @return true if load succeed
     */
    boolean load(final SnapshotReader reader);

    default boolean save(final RaftSnapshot value, String path) throws IOException {
        FileUtils.writeByteArrayToFile(new File(path), RaftSnapshotSerializer.encode(value));
        return true;
    }

    /**
     * Save value to snapshot file.
     */
    default Object load(String path) throws IOException {
        RaftSnapshot raftSnapshot = RaftSnapshotSerializer.decode(FileUtils.readFileToByteArray(new File(path)));
        return raftSnapshot.getBody();
    }



}
