package io.seata.compressor.gzip;

import io.seata.common.loader.LoadLevel;
import io.seata.core.compressor.Compressor;

/**
 * @author jsbxyyx
 */
@LoadLevel(name = "GZIP")
public class GzipCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] bytes) {
        return new byte[0];
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        return new byte[0];
    }

}
