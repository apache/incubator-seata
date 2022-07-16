package io.seata.server.console.service;

import io.seata.server.console.param.GlobalSessionParam;
import io.seata.server.console.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

/**
 *
 * @description: test for global session service
 * @author: Sher
 */

@SpringBootTest
public class GlobalSessionServiceTest {

    @MockBean
    GlobalSessionService globalSessionService;

    @Test
    public void testGlobalSessionQuery(){
        GlobalSessionParam globalSessionParam = new GlobalSessionParam();
        globalSessionParam.setXid("01");
        globalSessionParam.setPageNum(1);

        PageResult pageResult = new PageResult();
        pageResult.isSuccess();

        when(globalSessionService.query(globalSessionParam)).thenReturn(pageResult);
        assertThat(globalSessionService.query(globalSessionParam)).isEqualTo(pageResult);
//        Mockito.verify(globalSessionService).query(globalSessionParam);
    }
}
