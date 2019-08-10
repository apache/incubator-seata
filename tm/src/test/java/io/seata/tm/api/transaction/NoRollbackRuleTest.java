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
package io.seata.tm.api.transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
  * @author l81893521
  * @date 2019/8/9
  */
public class NoRollbackRuleTest {

    @Test
    public void equalsTest(){
        RollbackRule rollbackRuleByClass = new NoRollbackRule(Exception.class);
        RollbackRule otherRollbackRuleByClass = new NoRollbackRule(Exception.class);
        Assertions.assertEquals(rollbackRuleByClass, otherRollbackRuleByClass);
        RollbackRule rollbackRuleByName = new NoRollbackRule(Exception.class.getName());
        RollbackRule otherRollbackRuleByName = new NoRollbackRule(Exception.class.getName());
        Assertions.assertEquals(rollbackRuleByName, otherRollbackRuleByName);
    }
}
