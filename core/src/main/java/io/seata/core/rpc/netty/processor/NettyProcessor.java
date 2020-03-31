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
package io.seata.core.rpc.netty.processor;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.netty.AbstractRpcRemoting;

/**
 * The netty processor
 * <p>
 * Used to encapsulate remote interaction logic.
 * In order to separate the processing business from netty.
 * When netty starts, it will register processors to AbstractRpcRemoting#processorTable.
 * Call processor from {@link AbstractRpcRemoting#processMessage}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public interface NettyProcessor {

    /**
     * Process message
     *
     * @param ctx        Channel handler context.
     * @param rpcMessage rpc message.
     * @throws Exception throws exception process message error.
     */
    void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception;

}
