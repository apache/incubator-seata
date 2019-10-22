package io.seata.compressor.gzip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.zip.GZIPInputStream;

/**
 * @author jsbxyyx
 */
public class GzipUtilTest {

    @Test
    public void test_compress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.compress(null);
        });

        byte[] compress = GzipUtil.compress("aa".getBytes());
        int head = ((int) compress[0] & 0xff) | ((compress[1] << 8 ) & 0xff00);
        Assertions.assertEquals(GZIPInputStream.GZIP_MAGIC, head);
    }

    @Test
    public void test_decompress() {

        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.decompress(null);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            GzipUtil.decompress(new byte[0]);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            byte[] bytes = {0x1, 0x2};
            GzipUtil.decompress(bytes);
        });

    }

}
