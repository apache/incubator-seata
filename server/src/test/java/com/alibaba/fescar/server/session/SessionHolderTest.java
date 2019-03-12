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
package com.alibaba.fescar.server.session;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static com.alibaba.fescar.server.session.SessionHolder.ROOT_SESSION_MANAGER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Session holder test.
 *
 * @author Wu
 * @date 2019 /3/6 The type Session holder test.
 */
public class SessionHolderTest {

    /**
     * Test init.
     *
     * @throws IOException the io exception
     */
    @Test
    public void testInit() throws IOException {
        String sessionStorePath = System.getProperty("user.dir") + File.separator + "sessionStore";
        //delete file previously created
        File rootSessionFile = new File(sessionStorePath + File.separator + ROOT_SESSION_MANAGER_NAME);
        if (rootSessionFile.exists()) {
            rootSessionFile.delete();
        }
        File file = new File(sessionStorePath);
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
        SessionHolder.init(sessionStorePath);
        assertThat(new File(sessionStorePath + File.separator + ROOT_SESSION_MANAGER_NAME)).exists().isFile();
    }
}
