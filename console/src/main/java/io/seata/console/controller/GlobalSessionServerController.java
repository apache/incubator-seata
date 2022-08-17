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
package io.seata.console.controller;

import io.seata.console.param.GlobalSessionParam;
import io.seata.console.result.GlobalSessionVO;
import io.seata.console.result.PageResult;
import io.seata.console.utils.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: inner server http request of global session
 * @author: Sher
 */
@RestController
@RequestMapping("api/v1/console/globalSession")
public class GlobalSessionServerController {
    @Autowired
    private UrlUtils urlUtils;
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionServerController.class);

    /**
     * Query all globalSession
     *
     * @param param param for query globalSession
     * @return the list of GlobalSessionVO
     */
    @RequestMapping("query")
    PageResult<GlobalSessionVO> queryByXid(GlobalSessionParam param) {
        PageResult result = new PageResult();
        try {
            result = urlUtils.getPageResult();
        } catch (Exception e) {
            LOGGER.error("query global session information failed:", e);
        }
        return result;
    }
}
