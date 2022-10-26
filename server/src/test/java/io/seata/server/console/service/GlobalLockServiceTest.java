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
package io.seata.server.console.service;

import io.seata.server.console.param.GlobalLockParam;
import io.seata.server.console.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author: Sher
 * @description: test for global lock service
 */
@SpringBootTest
public class GlobalLockServiceTest {
    GlobalLockService globalLockService = mock(GlobalLockService.class);

    @Test
    public void testGlobalSessionQuery(){
        GlobalLockParam globalLockParam = new GlobalLockParam();
        globalLockParam.setPageNum(1);

        PageResult pageResult = new PageResult();
        pageResult.isSuccess();

        when(globalLockService.query(globalLockParam)).thenReturn(pageResult);
        assertThat(globalLockService.query(globalLockParam)).isEqualTo(pageResult);
    }
}
