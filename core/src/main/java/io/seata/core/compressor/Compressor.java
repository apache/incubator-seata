package io.seata.core.compressor;

/**
 * @author jsbxyyx
 */
public interface Compressor {

    /**
     * compress byte[] to byte[].
     * @param bytes the bytes
     * @return the byte [ ]
     */
    byte[] compress(byte[] bytes);

    /**
     * decompress byte[] to byte[].
     * @param bytes the bytes
     * @return the byte [ ]
     */
    byte[] decompress(byte[] bytes);

}
