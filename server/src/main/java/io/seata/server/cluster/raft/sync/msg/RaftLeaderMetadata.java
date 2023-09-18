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
package io.seata.server.cluster.raft.sync.msg;

import java.util.Map;
import java.util.Optional;
import io.seata.common.XID;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.metadata.ClusterRole;
import io.seata.common.metadata.Node;
import io.seata.common.util.StringUtils;
import org.springframework.core.env.Environment;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

/**
 * @author jianbin.chen
 */
public class RaftLeaderMetadata extends RaftBaseMsg {

	private static final long serialVersionUID = 6208583637662412658L;

	private Node node;
	
	private long term;

    public RaftLeaderMetadata(long term) {
        this(term, null);
    }

	public RaftLeaderMetadata() {
	}

	public RaftLeaderMetadata(long term, Map<String, Object> metadata) {
		this.term = term;
        Node node = new Node();
        node.setNettyEndpoint(node.createEndpoint(XID.getIpAddress(), XID.getPort()));
        node.setHttpEndpoint(node.createEndpoint(XID.getIpAddress(),
            Integer.parseInt(((Environment)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT))
                .getProperty("server.port", String.valueOf(8088)))));
	    node.setRole(ClusterRole.LEADER);
        Optional.ofNullable(metadata).ifPresent(node::setMetadata);
		this.msgType = RaftSyncMsgType.REFRESH_LEADER_METADATA;
        this.node = node;
    }

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public long getTerm() {
		return term;
	}

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
