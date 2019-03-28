package com.alibaba.fescar.core.rpc.netty;

import com.alibaba.fescar.common.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ensure the shutdownHook is singleton
 *
 * @author: 563868273@qq.com
 * @date: 2019/3/29
 */
public class FescarShutdownHook extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(FescarShutdownHook.class);

    private static final FescarShutdownHook fescarShutdownHook = new FescarShutdownHook("FescarShutdownHook");

    private Set<AbstractRpcRemoting> abstractRpcRemotings = new HashSet<>();

    private final AtomicBoolean destroyed= new AtomicBoolean(false);

    static {
        Runtime.getRuntime().addShutdownHook(fescarShutdownHook);
    }

    public FescarShutdownHook(String name) {
        super(name);
    }

    public static FescarShutdownHook getInstance(){
        return fescarShutdownHook;
    }

    public void addAbstractRpcRemoting(AbstractRpcRemoting abstractRpcRemoting){
        abstractRpcRemotings.add(abstractRpcRemoting);
    }

    @Override
    public void run() {
        destroyAll();
    }

    public void destroyAll() {
        if (!destroyed.compareAndSet(false, true) && CollectionUtils.isEmpty(abstractRpcRemotings)){
            return;
        }
        for (AbstractRpcRemoting abstractRpcRemoting : abstractRpcRemotings) {
            abstractRpcRemoting.destroy();
        }
    }

    /**
     * for spring context
     */
    public static void removeRuntimeShutdownHook(){
        Runtime.getRuntime().removeShutdownHook(fescarShutdownHook);
    }

}
