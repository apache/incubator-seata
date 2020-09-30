package io.seata.rm.datasource.util;

import io.seata.compressor.bzip2.BZip2Util;
import io.seata.compressor.gzip.GzipUtil;
import io.seata.compressor.lz4.Lz4Util;
import io.seata.compressor.sevenz.SevenZUtil;
import io.seata.compressor.zip.ZipUtil;
import io.seata.core.model.CompressType;

public class CompressUtil {
    public static byte[] compress(final byte[] bytes, CompressType compressType) {
        switch (compressType) {
            case GZIP:
                return GzipUtil.compress(bytes);
            case LZ4:
                return Lz4Util.compress(bytes);
            case ZIP:
                return ZipUtil.compress(bytes);
            case BZIP2:
                return BZip2Util.compress(bytes);
            case SEVEN_Z:
                return SevenZUtil.compress(bytes);
            case NONE:
            default:
                return bytes;
        }
    }

    public static byte[] decompress(final byte[] bytes, CompressType compressType) {
        switch (compressType) {
            case GZIP:
                return GzipUtil.decompress(bytes);
            case LZ4:
                return Lz4Util.decompress(bytes);
            case ZIP:
                return ZipUtil.decompress(bytes);
            case BZIP2:
                return BZip2Util.decompress(bytes);
            case SEVEN_Z:
                return SevenZUtil.decompress(bytes);
            default:
            case NONE:
                return bytes;
        }
    }
}
