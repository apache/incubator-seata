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
package io.seata.server.raft;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author funkye
 */
public class RaftSnapshotFile {

    private static final Logger LOG = LoggerFactory.getLogger(RaftSnapshotFile.class);

    private String path;

    public RaftSnapshotFile(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    /**
     * Save value to snapshot file.
     */
    public boolean save(final long value) {
        try {
            FileUtils.writeStringToFile(new File(path), String.valueOf(value));
            return true;
        } catch (IOException e) {
            LOG.error("Fail to save snapshot", e);
            return false;
        }
    }

    public long load() throws IOException {
        final String s = FileUtils.readFileToString(new File(path));
        if (!StringUtils.isBlank(s)) {
            return Long.parseLong(s);
        }
        throw new IOException("Fail to load snapshot from " + path + ",content: " + s);
    }
}
