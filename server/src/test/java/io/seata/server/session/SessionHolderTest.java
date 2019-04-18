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

package io.seata.server.session;

import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.StoreMode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static io.seata.server.session.SessionHolder.ROOT_SESSION_MANAGER_NAME;

/**
 * The type Session holder test.
 *
 * @author Wu
 * @date 2019 /3/6 The type Session holder test.
 */
public class SessionHolderTest {
    private String pathname;

    @Before
    public void before() {
        String sessionStorePath = SessionHolder.CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR);
        //delete file previously created
        pathname = sessionStorePath + File.separator + ROOT_SESSION_MANAGER_NAME;
    }

    @Test
    public void testInit() throws IOException {
        File rootSessionFile = new File(pathname);
        if (rootSessionFile.exists()) {
            rootSessionFile.delete();
        }
        final String mode = StoreMode.FILE.toString();
        SessionHolder.init(mode);
        final File actual = new File(pathname);
        Assert.assertTrue(actual.exists());
        Assert.assertTrue(actual.isFile());
    }

    @After
    public void after() {
        final File actual = new File(pathname);
        if (actual.exists()) {
            actual.delete();
        }
    }
}