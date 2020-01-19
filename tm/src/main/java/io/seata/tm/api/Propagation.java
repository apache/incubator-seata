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
package io.seata.tm.api;

public enum Propagation {
    /**
     * The REQUIRED.
     */
    REQUIRED("REQUIRED"),

    /**
     * The REQUIRES_NEW.
     */
    REQUIRES_NEW("REQUIRES_NEW"),

    /**
     * The NOT_SUPPORTED
     */
    NOT_SUPPORTED("NOT_SUPPORTED");


    private String name;

    Propagation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Propagation get(String name){
      for (Propagation propagation : Propagation.class.getEnumConstants()) {
                if (propagation.name.equals(name)) {
                    return propagation;
                }
            }
            throw new IllegalArgumentException("Unknown Propagation[" + name + "]");
    }
}

