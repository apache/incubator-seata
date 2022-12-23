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
package io.seata.rm.tcc.api;

import java.util.Map;

/**
 * the api of sharing business action context to tcc phase 2
 *
 * @author tanzj
 */
@Deprecated
public final class BusinessActionContextUtil {

    private BusinessActionContextUtil() {
    }

    /**
     * add business action context and share it to tcc phase2
     *
     * @param key   the key of new context
     * @param value new context
     * @return branch report succeed
     */
    public static boolean addContext(String key, Object value) {
        return io.seata.commonapi.api.BusinessActionContextUtil.addContext(key, value);
    }

    /**
     * batch share new context to tcc phase 2
     *
     * @param context the new context
     * @return branch report succeed
     */
    @SuppressWarnings("deprecation")
    public static boolean addContext(Map<String, Object> context) {
        return io.seata.commonapi.api.BusinessActionContextUtil.addContext(context);
    }

    /**
     * to do branch report sharing actionContext
     *
     * @param actionContext the context
     * @return branch report succeed
     */
    public static boolean reportContext(BusinessActionContext actionContext) {
        return io.seata.commonapi.api.BusinessActionContextUtil.reportContext(actionContext);
    }

    public static BusinessActionContext getContext() {
        return new BusinessActionContext(io.seata.commonapi.api.BusinessActionContextUtil.getContext());
    }

    public static void setContext(BusinessActionContext context) {
        io.seata.commonapi.api.BusinessActionContextUtil.setContext(context);
    }

    public static void clear() {
        io.seata.commonapi.api.BusinessActionContextUtil.clear();
    }

    /**
     * transfer tcc applicationData to BusinessActionContext
     *
     * @param xid             the xid
     * @param branchId        the branch id
     * @param resourceId      the resource id
     * @param applicationData the application data
     * @return business action context
     */
    public static BusinessActionContext getBusinessActionContext(String xid, long branchId, String resourceId, String applicationData) {
        return new BusinessActionContext(io.seata.commonapi.api.BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId, applicationData));
    }

    public static Object[] getTwoPhaseMethodParams(String[] keys, Class<?>[] argsClasses, BusinessActionContext businessActionContext) {
        return io.seata.commonapi.api.BusinessActionContextUtil.getTwoPhaseMethodParams(keys, argsClasses, businessActionContext);
    }
}