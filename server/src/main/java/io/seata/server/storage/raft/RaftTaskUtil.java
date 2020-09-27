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
package io.seata.server.storage.raft;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.entity.Task;
import io.seata.server.raft.RaftServerFactory;

import java.nio.ByteBuffer;

import static com.alipay.remoting.serialization.SerializerManager.Hessian2;

/**
 * @author funkye
 */
public class RaftTaskUtil {

    public static void createTask(Object data) {
        createTask(null, data);
    }

    public static void createTask(Closure done, Object data) {
        final Task task = new Task();
        try {
            task.setData(ByteBuffer.wrap(SerializerManager.getSerializer(Hessian2).serialize(data)));
        } catch (CodecException e) {
            e.printStackTrace();
        }
        task.setDone(done == null ? status -> {
        } : done);
        RaftServerFactory.getInstance().getRaftServer().getNode().apply(task);
    }

}
