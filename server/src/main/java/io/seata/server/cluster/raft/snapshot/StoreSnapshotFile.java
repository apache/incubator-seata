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
package io.seata.server.cluster.raft.snapshot;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;

/**
 * @author funkye
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

}
