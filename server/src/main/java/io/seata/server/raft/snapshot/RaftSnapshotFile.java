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
package io.seata.server.raft.snapshot;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author funkye
 */
public class RaftSnapshotFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftSnapshotFile.class);

    /**
     * Save value to snapshot file.
     */
    public static boolean save(final RaftSnapshot value, String path) {
        try {
            FileUtils.writeByteArrayToFile(new File(path), RaftSnapshotSerializer.encode(value));
            return true;
        } catch (IOException e) {
            LOGGER.error("Fail to save snapshot", e);
            return false;
        }
    }

    public static Map<String, Object> load(String path) throws IOException {
        RaftSnapshot raftSnapshot = RaftSnapshotSerializer.decode(FileUtils.readFileToByteArray(new File(path)));
        final Map<String, Object> map = (Map<String, Object>)raftSnapshot.getBody();
        if (!map.isEmpty()) {
            return map;
        }
        throw new IOException("Fail to load snapshot from " + path);
    }

}
