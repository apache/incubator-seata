package io.seata.core.codec;

/**
 * The interface Codec.
 *
 * @author zhangsen
 * @data 2019 /5/6
 */
public interface Codec {

    /**
     * Encode object to byte[].
     *
     * @param <T> the type parameter
     * @param t   the t
     * @return the byte [ ]
     */
    <T> byte[] encode(T t);

    /**
     * Decode t from byte[].
     *
     * @param <T>   the type parameter
     * @param bytes the bytes
     * @return the t
     */
    <T> T decode(byte[] bytes);
}
