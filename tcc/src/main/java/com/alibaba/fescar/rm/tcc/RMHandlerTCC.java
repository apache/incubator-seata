package com.alibaba.fescar.rm.tcc;

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.ResourceManager;
import com.alibaba.fescar.rm.AbstractRMHandler;
import com.alibaba.fescar.rm.DefaultResourceManager;

/**
 *
 * @author zhangsen
 */
public class RMHandlerTCC extends AbstractRMHandler {

    /**
     * get TCC resource manager
     * @return
     */
    @Override
    protected ResourceManager getResourceManager() {
        return DefaultResourceManager.get().getResourceManager(BranchType.TCC);
    }

    @Override
    public BranchType getBranchType(){
        return BranchType.TCC;
    }

}
