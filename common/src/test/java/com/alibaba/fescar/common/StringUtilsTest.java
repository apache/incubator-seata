/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.common;

import com.alibaba.fescar.common.util.StringUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import javax.sql.rowset.serial.SerialBlob;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author melon.zhao
 * @since 2019/2/20
 */
public class StringUtilsTest {

    @Test
    public void testIsEmpty() {
        Assert.assertEquals(StringUtils.isEmpty(null), true);
        Assert.assertEquals(StringUtils.isEmpty("abc"), false);
        Assert.assertEquals(StringUtils.isEmpty(""), true);
        Assert.assertEquals(StringUtils.isEmpty(" "), false);
    }

    @Test
    public void testString2blob() throws SQLException {
        Assert.assertEquals(StringUtils.string2blob(null), null);
        String[] strs = new String[]{"abc", "", " "};
        for (String str : strs) {
            Assert.assertEquals(StringUtils.string2blob(str), new SerialBlob(str.getBytes()));
        }
    }

    @Test
    public void testBlob2string() throws SQLException {
        String[] strs = new String[]{"abc", " "};
        for (String str : strs) {
            Assert.assertEquals(StringUtils.blob2string(new SerialBlob(str.getBytes())), str);
        }
    }

    @Test
    public void testInputStream2String() {
        try {
            InputStream inputStream = StringUtilsTest.class.getClassLoader().getResourceAsStream("test.txt");
            Assert.assertEquals(StringUtils.inputStream2String(inputStream), "abc\n"
                + ":\"klsdf\n"
                + "2ks,x:\".,-3sd˚ø≤ø¬≥");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
