package io.seata.common.util;

import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

/**
 * @description:
 * @author: zhangjiawei
 * @date: 2023/12/22
 */
public class HttpClientUtilTest {

    @Test
    public void testDoPost() throws IOException {
        Assertions.assertNull(HttpClientUtil.doPost("test", new HashMap<>(), new HashMap<>(), 0));
        Assertions.assertNull(HttpClientUtil.doGet("test", new HashMap<>(), new HashMap<>(), 0));
    }
}
