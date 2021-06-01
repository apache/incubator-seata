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
import javax.annotation.Nonnull;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.tcc.interceptor.ActionContextUtil;

/**
 * The default ParameterFetcher.
 *
 * @author wang.liang
 * @since above 1.4.2
 */
public class DefaultParameterFetcher implements ParameterFetcher {

    @Override
    public void fetchContext(@Nonnull Object objValue, @Nonnull Map<String, Object> actionContext, @Nonnull BusinessActionContextParameter annotation) {
        Map<String, Object> paramContext = ActionContextUtil.fetchContextFromObject(objValue);
        if (CollectionUtils.isEmpty(paramContext)) {
            return;
        }
        String paramName = ActionContextUtil.getParamName(annotation);
        if (StringUtils.isNotBlank(paramName)) {
            // If the `paramName` of "@BusinessActionContextParameter" is not blank, put the param context in it
            // @since: above 1.4.2
            actionContext.put(paramName, paramContext);
        } else {
            // Merge the param context into context
            // Warn: This may cause values with the same name to be overridden
            actionContext.putAll(paramContext);
        }
    }
}
