package io.seata.core.protocol;

import org.junit.jupiter.api.*;

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

	private static RegisterTMRequest RTMR;
	private static final String APP_ID = "applicationId";
	private static final String TSG = "transactionServiceGroup";
	private static final String ED = "extraData";
	
	/** Constructor without arguments **/
	@BeforeAll
	public static void setupNull() {
		RTMR = new RegisterTMRequest();
	}
	
	/** Constructor with arguments **/
	@BeforeAll
	public static void setupWithValues() {
		RTMR = new RegisterTMRequest(APP_ID, TSG, ED);
	}
	
	/** Test get type code **/
	@Test
	public void testGetTypeCode() {
		Assertions.assertEquals(101, RTMR.getTypeCode());
	}
	
	/**
	 * Test toString having all the parameters initialized to null
	 */
	@Test
	public void testToStringNullValues() {
		Assertions.assertEquals(
				"RegisterTMRequest{" + "applicationId='" + null + '\'' +
			            ", transactionServiceGroup='" + null + '\'' +
			            '}',
				RTMR.toString());
	}
		
}
