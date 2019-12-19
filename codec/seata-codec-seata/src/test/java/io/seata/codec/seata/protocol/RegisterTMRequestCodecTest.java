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
package io.seata.codec.seata.protocol;

import io.netty.buffer.ByteBuf;
import io.seata.codec.seata.SeataCodec;
import io.seata.core.protocol.AbstractIdentifyRequest;
import io.seata.core.protocol.RegisterTMRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.netty.buffer.Unpooled.buffer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Register tm request codec test.
 *
 * @author zhangsen
 */
public class RegisterTMRequestCodecTest {

    /**
     * The Seata codec.
     */
    SeataCodec seataCodec = new SeataCodec();

    private static       RegisterTMRequest       registerTMRequest;
    private static       AbstractIdentifyRequest air;
    private static final String                  APP_ID    = "applicationId";
    private static final String                  TSG       = "transactionServiceGroup";
    private static final String                  ED        = "extraData";
    private static final short                   TYPE_CODE = 101;
    private static final ByteBuf                 BB        = buffer(128);

    /**
     * Test codec.
     */
    @Test
    public void test_codec() {
        RegisterTMRequest registerTMRequest = new RegisterTMRequest();
        registerTMRequest.setApplicationId("abc");
        registerTMRequest.setExtraData("abc123");
        registerTMRequest.setTransactionServiceGroup("def");
        registerTMRequest.setVersion("1");

        byte[] body = seataCodec.encode(registerTMRequest);

        RegisterTMRequest registerTMRequest2 = seataCodec.decode(body);

        assertThat(registerTMRequest2.getApplicationId()).isEqualTo(registerTMRequest.getApplicationId());
        assertThat(registerTMRequest2.getExtraData()).isEqualTo(registerTMRequest.getExtraData());
        assertThat(registerTMRequest2.getTransactionServiceGroup()).isEqualTo(registerTMRequest.getTransactionServiceGroup());
        assertThat(registerTMRequest2.getVersion()).isEqualTo(registerTMRequest.getVersion());
    }

    /**
     * Constructor without arguments
     **/
    @BeforeAll
    public static void setupNull() {
        registerTMRequest = new RegisterTMRequest();
        air = Mockito.mock(
                AbstractIdentifyRequest.class,
                Mockito.CALLS_REAL_METHODS);
    }

    /**
     * Constructor with arguments
     **/
    @BeforeAll
    public static void setupWithValues() {
        registerTMRequest = new RegisterTMRequest(APP_ID, TSG, ED);
        air = Mockito.mock(
                AbstractIdentifyRequest.class,
                Mockito.CALLS_REAL_METHODS);
    }

    /**
     * Test get type code
     **/
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
        try {
            seataCodec.decode(BB.array());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("not support "), "error data");
        }
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
        try {
            seataCodec.decode(BB.array());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("not support "), "error data");
        }
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
        try {
            seataCodec.decode(BB.array());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("not support "), "error data");
        }
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
        try {
            seataCodec.decode(BB.array());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("not support "), "error data");
        }
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
        try {
            seataCodec.decode(BB.array());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("not support "), "error data");
        }
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

        try {
            seataCodec.decode(BB.array());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("not support "), "error data");
        }
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
        try {
            seataCodec.decode(BB.array());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("not support "), "error data");
        }
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
        try {
            seataCodec.decode(BB.array());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("not support "), "error data");
        }
    }
}
