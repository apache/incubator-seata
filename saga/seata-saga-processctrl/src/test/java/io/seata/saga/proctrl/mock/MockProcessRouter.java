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
package io.seata.saga.proctrl.mock;

import io.seata.common.exception.FrameworkException;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.ProcessRouter;

/**
 *
 * @author lorne.cl
 */
public class MockProcessRouter implements ProcessRouter {

    @Override
    public Instruction route(ProcessContext context) throws FrameworkException {
        System.out.println("MockProcessRouter.route executed. context: " + context);
        MockInstruction instruction  = context.getInstruction(MockInstruction.class);
        if(instruction != null){
            if("one".equals(instruction.getTestString())){
                instruction.setTestString("two");
            }
            else if("two".equals(instruction.getTestString())){
                instruction.setTestString("three");
            }
            else{
                instruction.setTestString(null);
                return null;//end process
            }
        }
        return instruction;
    }
}