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
import io.seata.console.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @description: test for global session server controller
 * @author: Sher
 */
public class GlobalSessionServerControllerTest {
    MockMvc mockMvc;
    GlobalSessionServerController globalSessionServerController = mock(GlobalSessionServerController.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(GlobalSessionServerController.class).build();
    }

    @Test
    public void testQuery() throws Exception {
        GlobalSessionParam globalSessionParam = new GlobalSessionParam();
        globalSessionParam.setPageNum(1);
        PageResult pageResult = new PageResult();
        pageResult.isSuccess();
        Mockito.when(globalSessionServerController.query(globalSessionParam))
                .thenReturn(pageResult);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/console/globalSession/query?withBranch=true&pageSize=10&pageNum=1").contentType(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
