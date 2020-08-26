package io.seata.common.util;

import io.seata.common.BranchDO;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The bean utils test
 *
 * @author wangzhongxiang
 */
public class BeanUtilsTest {

    @Test
    public void testMapToObject(){
        Map<String,String> map = new HashMap<>();
        Date date = new Date();
        map.put("xid","192.166.166.11:9010:12423424234234");
        map.put("transactionId","12423424234234");
        map.put("status","2");
        map.put("test","22.22");
        map.put("gmtCreate",String.valueOf(date.getTime()));
        BranchDO branchDO =
                (BranchDO)BeanUtils.mapToObject(map, BranchDO.class);
        Assertions.assertEquals("192.166.166.11:9010:12423424234234",branchDO.getXid());
        Assertions.assertEquals(12423424234234L,branchDO.getTransactionId());
        Assertions.assertEquals(2,branchDO.getStatus());
        Assertions.assertEquals(new Date(date.getTime()),branchDO.getGmtCreate());

    }


}
