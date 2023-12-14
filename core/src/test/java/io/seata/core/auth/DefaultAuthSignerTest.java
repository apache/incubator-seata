package io.seata.core.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The DefaultAuthSigner Test
 *
 * @author zhongxiang.wang
 */
public class DefaultAuthSignerTest {
    @Test
    public void testGetRamSignNotNull() {
        String data = "testGroup,127.0.0.1,1702564471650";
        String key = "exampleEncryptKey";
        String expectedSign = "6g9nMk6BRLFxl7bf5ZfWaEZvGdho3JBmwvx5rqgSUCE=";

        DefaultAuthSigner signer = new DefaultAuthSigner();
        String sign = signer.sign(data, key);
        Assertions.assertEquals(expectedSign, sign);
    }

    @Test
    public void testGetRamSignNull() {
        String data = null;
        String key = "exampleEncryptKey";

        DefaultAuthSigner signer = new DefaultAuthSigner();
        String sign = signer.sign(data, key);
        Assertions.assertNull(sign);
    }

    @Test
    public void testGetSignVersion() {
        DefaultAuthSigner signer = new DefaultAuthSigner();
        String expectedVersion = "V4";
        String actualVersion = signer.getSignVersion();

        // Assert the returned version matches the expected version
        Assertions.assertEquals(expectedVersion, actualVersion);
    }
}
