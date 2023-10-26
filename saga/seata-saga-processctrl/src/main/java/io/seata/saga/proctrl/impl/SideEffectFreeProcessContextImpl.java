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

package io.seata.saga.proctrl.impl;

/**
 * Side-effect-free process context which manage variable locally.
 *
 * @author ptyin
 */
public class SideEffectFreeProcessContextImpl extends ProcessContextImpl {
    @Override
    public void setVariable(String name, Object value) {
        setVariableLocally(name, value);
    }

    @Override
    public Object removeVariable(String name) {
        return removeVariableLocally(name);
    }

}
