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


/**
 * @author guoyao
 */
public class NoRollbackRule extends RollbackRule {

    public static final NoRollbackRule DEFAULT_NO_ROLLBACK_RULE = new NoRollbackRule(Throwable.class);


    public NoRollbackRule(Class<?> clazz) {
        super(clazz);
    }


    public NoRollbackRule(String exceptionName) {
        super(exceptionName);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "No" + super.toString();
    }
}
