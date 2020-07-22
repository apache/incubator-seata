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
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author : wangxb
 * @Description: Mock springmvc controller
 */
@Controller
public class MockController {


    @RequestMapping("/testGet")
    @ResponseBody
    public String testGet(HttpTest.Person person) {
        /* verify xid propagate by test case */
        Assertions.assertEquals(HttpTest.XID,RootContext.getXID());
        return person.toString();
    }


    @ResponseBody
    @PostMapping("/testPost")
    public String testPost(@RequestBody HttpTest.Person person) {
        /* verify xid propagate by test case */
        Assertions.assertEquals(HttpTest.XID,RootContext.getXID());
        return person.toString();
    }

    @RequestMapping("/testException")
    @ResponseBody
    public String testException(HttpTest.Person person) {
        throw new RuntimeException();
    }

}
