package io.seata.server.console.service;

import io.seata.server.console.param.GlobalLockParam;
import io.seata.server.console.param.GlobalSessionParam;
import io.seata.server.console.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

/**
 *
 * @author: Sher
 * @description: test for global lock service
 */
@SpringBootTest
public class GlobalLockServiceTest {
    @MockBean
    GlobalLockService globalLockService;

    @Test
    public void testGlobalSessionQuery(){
//        GlobalSessionParam globalSessionParam = new GlobalSessionParam();
        GlobalLockParam globalLockParam = new GlobalLockParam();
        globalLockParam.setPageNum(1);

        PageResult pageResult = new PageResult();
        pageResult.isSuccess();

        when(globalLockService.query(globalLockParam)).thenReturn(pageResult);
        assertThat(globalLockService.query(globalLockParam)).isEqualTo(pageResult);
//        Mockito.verify(globalSessionService).query(globalSessionParam);
    }
}
