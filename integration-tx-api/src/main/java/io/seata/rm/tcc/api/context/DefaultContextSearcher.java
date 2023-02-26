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
package io.seata.rm.tcc.api.context;

import io.seata.common.Constants;
import io.seata.common.ContextStoreConstant;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.tcc.api.BusinessActionContext;

/**
 * default ContextSearcher
 *
 * @author yangwenpeng
 */
public class DefaultContextSearcher implements ContextSearcher {
    private static final ContextSearcher DEFAULT_SEARCHER = EnhancedServiceLoader.load(ContextSearcher.class, ContextStoreConstant.STORE_TYPE_TC);

    @Override
    public BusinessActionContext search(BusinessActionContext contextFromTc) {
        ContextSearcher contextSearcher = EnhancedServiceLoader.load(ContextSearcher.class
            , contextFromTc.getActionContext(Constants.TCC_ACTION_CONTEXT_REPORT_TYPE, String.class));
        if (!contextSearcher.isSupport(contextFromTc)) {
            return DEFAULT_SEARCHER.search(contextFromTc);
        }
        // do search
        return contextSearcher.search(contextFromTc);
    }

    @Override
    public boolean isSupport(BusinessActionContext context) {
        ContextSearcher contextSearcher = EnhancedServiceLoader.load(ContextSearcher.class
            , context.getActionContext(Constants.TCC_ACTION_CONTEXT_REPORT_TYPE, String.class));
        return contextSearcher.isSupport(context);
    }

    /**
     * Get default context searcher.
     *
     * @return the default context searcher.
     */
    public static DefaultContextSearcher get() {
        return DefaultContextSearcher.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static DefaultContextSearcher INSTANCE = new DefaultContextSearcher();
    }
}
