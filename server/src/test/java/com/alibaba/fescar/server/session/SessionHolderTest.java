package com.alibaba.fescar.server.session;


import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Wu
 * @date 2019/3/6
 * xingfudeshi@gmail.com
 * The type Session holder test.
 */
public class SessionHolderTest {

    @Test
    public void testInit() throws IOException {
        String sessionStorePath=System.getProperty("user.dir")+ File.separator +"sessionStore";
        File file=new File(sessionStorePath);
        if(!file.exists()&&!file.isDirectory()){
            file.mkdirs();
        }
        SessionHolder.init(sessionStorePath);
    }
}
