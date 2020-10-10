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
package io.seata.core.rpc.processor.server;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xhe 774630093@qq.com
 * @classname AbstractRemotingProcessor
 * @description AbstractRemotingProcessor
 * @date 2020/10/10 16:22
 */
public abstract class AbstractRemotingProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRemotingProcessor.class);

    protected void close(ChannelHandlerContext ctx) {
        try {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("closeChannelHandlerContext channel:" + ctx.channel());
            }
            ctx.disconnect();
            ctx.close();
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("close a unhandled connection! [%s]", ctx.channel().toString()));
        }
    }
}
