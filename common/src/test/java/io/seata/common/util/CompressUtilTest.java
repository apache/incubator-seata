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
package io.seata.common.util;

import java.io.EOFException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CompressUtilTest {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testCompress() throws Exception {
		byte[] expected = {31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 75, -53, -49, 119, 74, 44, 2, 0, 117, 89, -69, -90, 6, 0, 0, 0};

		Assert.assertArrayEquals(expected, CompressUtil.compress(new byte[] {'f', 'o', 'o', 'B', 'a', 'r'}));
	}

	@Test
	public void testUncompress() throws Exception {
		final byte[] bytes = {31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 75, -53, -49, 119, 74, 44, 2, 0, 117, 89, -69, -90, 6, 0, 0, 0};

		Assert.assertArrayEquals(new byte[] {'f', 'o', 'o', 'B', 'a', 'r'}, CompressUtil.uncompress(bytes));

		thrown.expect(EOFException.class);
		CompressUtil.uncompress(new byte[] {});
	}

	@Test
	public void testIsCompressData() {
		Assert.assertFalse(CompressUtil.isCompressData(null));
		Assert.assertFalse(CompressUtil.isCompressData(new byte[] {}));
		Assert.assertFalse(CompressUtil.isCompressData(new byte[] {31, 11, 0}));

		Assert.assertTrue(CompressUtil.isCompressData(new byte[] {31, -117, 0}));
	}
}
