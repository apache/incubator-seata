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
