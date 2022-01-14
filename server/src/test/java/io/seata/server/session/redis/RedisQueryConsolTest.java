package io.seata.server.session.redis;


import com.alibaba.fastjson.JSON;
import io.seata.core.console.param.GlobalLockParam;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.result.PageResult;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.console.service.GlobalSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author doubleDimple
 */
@SpringBootTest
public class RedisQueryConsolTest {


    /**
     *
     *if you test globalSessionService and  globalLockService
     *please update config information ths file: application.yml
     *
     * store:
     *     # support: file 、 db 、 redis
     *     mode: redis
     *     redis:
     *       mode: single
     *       database: 0
     *       min-conn: 1
     *       max-conn: 10
     *       password:
     *       max-total: 100
     *       query-limit: 100
     *       single:
     *         host: real redis host
     *         port: 6879
     */


    @Resource
    private GlobalSessionService globalSessionService;

    @Resource
    private GlobalLockService globalLockService;


    @Test
    public void test_globalRedisServiceQuery(){
        GlobalSessionParam param = new GlobalSessionParam();

        PageResult<GlobalSessionVO> query = globalSessionService.query(param);
        System.out.print(JSON.toJSON(query));
    }


    @Test
    public void test_queryGlobalLock(){
        GlobalLockParam param = new GlobalLockParam();
        globalLockService.query(param);
    }
}
