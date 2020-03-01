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
package io.seata.compressor.zip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * the Zip Util test
 *
 * @author ph3636
 */
public class ZipUtilTest {

    @Test
    public void test_compress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ZipUtil.compress(null);
        });
    }

    @Test
    public void test_decompress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ZipUtil.decompress(null);
        });
    }
}
