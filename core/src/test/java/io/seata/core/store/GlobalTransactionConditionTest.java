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
package io.seata.core.store;

import io.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wang.liang
 */
public class GlobalTransactionConditionTest {

    @Test
    public void isMatchTest() throws InterruptedException {
        GlobalTransactionCondition condition = new GlobalTransactionCondition();
        condition.setStatuses(GlobalStatus.Begin);
        condition.setOverTimeAliveMills(10);
        condition.setTimeoutData(true);

        GlobalTransactionDO obj = new GlobalTransactionDO();
        obj.setStatus(GlobalStatus.Begin.getCode());
        obj.setBeginTime(System.currentTimeMillis());
        obj.setTimeout(100);

        Assertions.assertFalse(condition.isMatch(obj));
        Thread.sleep(101);
        Assertions.assertTrue(condition.isMatch(obj));

        condition.setTimeoutData(false);
        Assertions.assertFalse(condition.isMatch(obj));
        condition.setTimeoutData(true);

        condition.setOverTimeAliveMills(2000);
        Assertions.assertFalse(condition.isMatch(obj));
        condition.setOverTimeAliveMills(10);
    }

    @Test
    public void doSortTest() {
        List<GlobalTransactionDO> list0 = new ArrayList<>();

        GlobalTransactionDO obj1 = new GlobalTransactionDO();
        obj1.setStatus(GlobalStatus.Begin.getCode());
        list0.add(obj1);

        GlobalTransactionDO obj2 = new GlobalTransactionDO();
        obj2.setStatus(GlobalStatus.UnKnown.getCode());
        list0.add(obj2);

        GlobalTransactionDO obj3 = new GlobalTransactionDO();
        obj3.setStatus(GlobalStatus.Finished.getCode());
        list0.add(obj3);

        GlobalTransactionDO obj4 = new GlobalTransactionDO();
        obj4.setStatus(GlobalStatus.AsyncCommitting.getCode());
        list0.add(obj4);

        int size = list0.size();


        GlobalTransactionCondition condition = new GlobalTransactionCondition();
        condition.setSortFieldName(GlobalTableField.STATUS.getFieldName());

        List<GlobalTransactionDO> list1 = condition.doSort(list0);

        Assertions.assertEquals(list1.size(), size);
        Assertions.assertEquals(list1.get(0).getStatus(), GlobalStatus.UnKnown.getCode());
        Assertions.assertEquals(list1.get(1).getStatus(), GlobalStatus.Begin.getCode());
        Assertions.assertEquals(list1.get(2).getStatus(), GlobalStatus.AsyncCommitting.getCode());
        Assertions.assertEquals(list1.get(3).getStatus(), GlobalStatus.Finished.getCode());
    }

    @Test
    public void doPagingTest() {
        List<GlobalTransactionDO> list0 = new ArrayList<>();

        GlobalTransactionDO obj1 = new GlobalTransactionDO();
        obj1.setStatus(GlobalStatus.Begin.getCode());
        list0.add(obj1);

        GlobalTransactionDO obj2 = new GlobalTransactionDO();
        obj2.setStatus(GlobalStatus.UnKnown.getCode());
        list0.add(obj2);

        GlobalTransactionDO obj3 = new GlobalTransactionDO();
        obj3.setStatus(GlobalStatus.Finished.getCode());
        list0.add(obj3);

        GlobalTransactionDO obj4 = new GlobalTransactionDO();
        obj4.setStatus(GlobalStatus.AsyncCommitting.getCode());
        list0.add(obj4);

        GlobalTransactionCondition condition = new GlobalTransactionCondition();
        condition.setPageIndex(2);
        condition.setPageSize(3);
        List<GlobalTransactionDO> list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 1);
        Assertions.assertEquals(list1.get(0).getStatus(), GlobalStatus.AsyncCommitting.getCode());

        condition.setPageIndex(3);
        condition.setPageSize(3);
        list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 0);

        condition.setPageIndex(1);
        condition.setPageSize(4);
        list1 = condition.doPaging(list0);
        Assertions.assertEquals(list1.size(), 4);
        Assertions.assertEquals(list1.get(0).getStatus(), GlobalStatus.Begin.getCode());
        Assertions.assertEquals(list1.get(1).getStatus(), GlobalStatus.UnKnown.getCode());
        Assertions.assertEquals(list1.get(2).getStatus(), GlobalStatus.Finished.getCode());
        Assertions.assertEquals(list1.get(3).getStatus(), GlobalStatus.AsyncCommitting.getCode());
    }

}
