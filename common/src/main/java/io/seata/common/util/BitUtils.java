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
package io.seata.common.util;

/**
 * @author : MentosL
 * @date : 2023/3/2 11:46
 */
public class BitUtils {


    /**
     * Add a value to a certain position of an int type constant
     *
     * @param value
     * @param bitTag
     * @return
     */
    public static int setBit(int value, int bitTag) {
        return value | bitTag;
    }

    /**
     * Remove the value for a certain position of the int type constant
     *
     * @param value
     * @param bitTag
     * @return
     */
    public static int unSetBit(int value, int bitTag) {
        return value ^ bitTag;
    }


    /**
     * Determine whether a certain position of an int type constant is set
     * @param value
     * @param bitTag
     * @return
     */
    public static boolean isSetBit(int value, int bitTag) {
        return (value & bitTag) == bitTag;
    }

}
