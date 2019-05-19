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
package io.seata.core.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.buffer;

/**
 * RegisterTMRequest Test
 * 
 * @author kaitithoma
 * @author Danaykap
 * 
 * @date 2019/05/13
 *
 */

public class RegisterTMRequestTest {

	private static RegisterTMRequest registerTMRequest;
	private static AbstractIdentifyRequest air;
	private static final String APP_ID = "applicationId";
	private static final String TSG = "transactionServiceGroup";
	private static final String ED = "extraData";
	private static final short TYPE_CODE = 101;
	private static final ByteBuf BB = buffer(128);
	
	/** Constructor without arguments **/
	@BeforeAll
	public static void setupNull() {
		registerTMRequest = new RegisterTMRequest();
		air = Mockito.mock(
				AbstractIdentifyRequest.class, 
			    Mockito.CALLS_REAL_METHODS);
	}

	/** Constructor with arguments **/
	@BeforeAll
	public static void setupWithValues() {
		registerTMRequest = new RegisterTMRequest(APP_ID, TSG, ED);
		air = Mockito.mock(
				AbstractIdentifyRequest.class, 
			    Mockito.CALLS_REAL_METHODS);
	}

	/** Test get type code **/
	@Test
	public void testGetTypeCode() {
		Assertions.assertEquals(TYPE_CODE, registerTMRequest.getTypeCode());
	}

	/**
	 * Test toString having all the parameters initialized to null
	 */
	@Test
	public void testToStringNullValues() {
		Assertions.assertEquals("RegisterTMRequest{" + "applicationId='" + null + '\'' + ", transactionServiceGroup='"
				+ null + '\'' + '}', registerTMRequest.toString());
	}

	/**
	 * Test decode method with empty parameter
	 */
	@Test
	public void testDecodeEmpty() {
		BB.clear();
		Assertions.assertFalse(air.decode(BB));
	}
	
	/**
	 * Test decode method with initialized parameter
	 */
	@Test
	public void testDecodeLessThanTwo() {
		BB.clear();
		for (int i = 0; i < 2; i++) {
			BB.writeShort(i);
		}
		Assertions.assertFalse(air.decode(BB));
	}
	
	/**
	 * Test decode method with initialized parameter
	 */
	@Test
	public void testDecodeMoreThanThree() {
		BB.clear();
		for (int i = 0; i < 3; i++) {
			BB.writeShort(i);
		}
		Assertions.assertFalse(air.decode(BB));
	}
	
	/**
	 * Test decode method with initialized parameter
	 */
	@Test
	public void testDecodeLessThanFour() {
		BB.clear();
		for (int i = 0; i < 4; i++) {
			BB.writeShort(i);
		}
		Assertions.assertFalse(air.decode(BB));
	}
	
	/**
	 * Test decode method with initialized parameter
	 */
	@Test
	public void testDecodeMoreLessThanOne() {
		BB.clear();
		for (int i = 0; i < 1; i++) {
			BB.writeShort(i);
		}
		Assertions.assertFalse(air.decode(BB));
	}
	
	/**
	 * Test decode method with initialized parameter
	 */
	@Test
	public void testDecodeMoreLessThanFourWithZero() {
		BB.clear();
		for (int i = 0; i < 4; i++) {
			BB.writeZero(i);
		}
		Assertions.assertFalse(air.decode(BB));
	}
	
	/**
	 * Test decode method with initialized parameter
	 */
	@Test
	public void testDecodeFalseLessThanTwoWithDifferentReadable() {
		BB.clear();
		for (int i = 0; i < 1; i++) {
			BB.writeZero(i);
		}
		for (int i = 1; i < 2; i++) {
			BB.writeShort(i);
		}
		Assertions.assertFalse(air.decode(BB));
	}
	
	/**
	 * Test decode method with initialized parameter
	 */
	@Test
	public void testDecodeTrueLessThanFive() {
		BB.clear();
		for (int i = 0; i < 4; i++) {
			BB.writeZero(i);
		}
		for (int i = 4; i < 5; i++) {
			BB.writeShort(i);
		}
		Assertions.assertTrue(air.decode(BB));
	}
	
	/**
	 * Test decode method with initialized parameter
	 */
	@Test
	public void testDecodeTrueLessThanSixteen() {
		BB.clear();
		for (int i = 0; i < 15; i++) {
			BB.writeZero(i);
		}
		for (int i = 15; i < 16; i++) {
			BB.writeShort(i);
		}
		Assertions.assertTrue(air.decode(BB));
	}
	
}
