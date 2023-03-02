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
