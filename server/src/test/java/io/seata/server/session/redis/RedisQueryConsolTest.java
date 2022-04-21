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
package io.seata.server.session.redis;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import io.seata.console.result.PageResult;
import io.seata.server.console.param.GlobalLockParam;
import io.seata.server.console.param.GlobalSessionParam;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.console.service.GlobalSessionService;
import io.seata.server.console.vo.GlobalLockVO;
import io.seata.server.console.vo.GlobalSessionVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author doubleDimple
 */
@SpringBootTest
public class RedisQueryConsolTest {

    /**
     *
     *if you want test redis mode globalSessionService and  globalLockService
     *
     *please update config information ths file: application.yml
     * store:
          # support: file 、 db 、 redis
          mode: redis
          redis:
            mode: single
            database: 0
            min-conn: 1
            max-conn: 10
            password:
            max-total: 100
            query-limit: 100
            single:
              host: real redis host
              port: 6879
     *!!!!!!!when you test finish,please restore the modified configuration!!!!!!!!
     */
    @Resource
    private GlobalSessionService globalSessionService;

    @Resource
    private GlobalLockService globalLockService;

    @Test
    public void test_globalRedisServiceQuery() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(4);
        param.setXid("SEATA_GLOBAL_LOCK_192.168.158.80:8091:37621364385185792");
        PageResult<GlobalSessionVO> query = globalSessionService.query(param);
        System.out.print(JSON.toJSON(query));
    }

    @Test
    public void test_queryGlobalLock() {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageSize(2);
        param.setPageNum(1);
        param.setXid("_192.168.158.80:8091:37621364385185792");
        try {
            PageResult<GlobalLockVO> query = globalLockService.query(param);
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
    }
}
