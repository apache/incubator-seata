package com.alibaba.fescar.common.util;

import java.io.InputStream;
import java.sql.SQLException;
import javax.sql.rowset.serial.SerialBlob;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * @author melon.zhao
 * @since 2019/2/26
 */
public class BlobUtilsTest {

    @Test
    public void testString2blob() throws SQLException {
        Assert.assertEquals(BlobUtils.string2blob("123abca"),new SerialBlob("123abca".getBytes()));
    }

    @Test
    public void testBlob2string() throws SQLException {
        Assert.assertEquals(BlobUtils.blob2string(new SerialBlob("123abckdle".getBytes())),"123abckdle");
    }

    @Test
    public void testInputStream2String() {
        InputStream inputStream = StringUtilsTest.class.getClassLoader().getResourceAsStream("test.txt");
        Assert.assertEquals(BlobUtils.inputStream2String(inputStream),"abc\n"
            + ":\"klsdf\n"
            + "2ks,x:\".,-3sd˚ø≤ø¬≥");
    }
}