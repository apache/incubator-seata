package com.alibaba.fescar.server.session;


import org.testng.annotations.Test;

import java.io.IOException;
/**
 * @author Wu
 * @date 2019/3/6
 * xingfudeshi@gmail.com
 * The type Session holder test.
 */
public class SessionHolderTest {

    @Test
    public void testInit() throws IOException {
        SessionHolder.init("D:\\sessionStore");
    }
}
