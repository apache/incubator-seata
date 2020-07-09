package io.seata.common.util;

/**
 * The type Comparable Utils.
 *
 * @author wang.liang
 */
public class ComparableUtils {

    private ComparableUtils() {
    }

    /**
     * Compare to
     *
     * @param a the comparable object a
     * @param b the comparable object b
     * @return 0: equals    -1: a < b    1: a > b
     */
    public static int compareTo(Comparable a, Comparable b) {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }

        return a.compareTo(b);
    }
}
