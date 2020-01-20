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
package io.seata.integration.http;

import io.seata.core.context.RootContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Http exception handle.
 *
 * @author wangxb
 */
public class HttpHandlerExceptionResolver extends AbstractHandlerExceptionResolver {


    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse httpServletResponse, Object o, Exception e) {

        XidResource.cleanXid(request.getHeader(RootContext.KEY_XID));
        return null;
    }
}
