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

import io.seata.common.util.StringUtils;

import java.io.Serializable;

/**
 * @author guoyao
 */
public class RollbackRule implements Serializable {


    private final String exceptionName;

    public RollbackRule(String exceptionName) {
        if (StringUtils.isNullOrEmpty(exceptionName)) {
            throw new IllegalArgumentException("'exceptionName' cannot be null or empty");
        }
        this.exceptionName = exceptionName;
    }

    public RollbackRule(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("'clazz' cannot be null");
        }
        if (!Throwable.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(
                    "Cannot construct rollback rule from [" + clazz.getName() + "]: it's not a Throwable");
        }
        this.exceptionName = clazz.getName();
    }

    public String getExceptionName() {
        return this.exceptionName;
    }


    public int getDepth(Throwable ex) {
        return getDepth(ex.getClass(), 0);
    }


    private int getDepth(Class<?> exceptionClass, int depth) {
        if (exceptionClass.getName().contains(this.exceptionName)) {
            // Found it!
            return depth;
        }
        // If we've gone as far as we can go and haven't found it...
        if (exceptionClass == Throwable.class) {
            return -1;
        }
        return getDepth(exceptionClass.getSuperclass(), depth + 1);
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RollbackRule)) {
            return false;
        }
        RollbackRule rhs = (RollbackRule) other;
        return this.exceptionName.equals(rhs.exceptionName);
    }

    @Override
    public int hashCode() {
        return this.exceptionName.hashCode();
    }

    @Override
    public String toString() {
        return "RollbackRule with pattern [" + this.exceptionName + "]";
    }
}
