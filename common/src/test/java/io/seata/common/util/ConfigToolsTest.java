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

import java.security.KeyPair;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author funkye
 */
public class ConfigToolsTest {

    @Test
    public void test() throws Exception {
        KeyPair keyPair = ConfigTools.getKeyPair();
        String publicKeyStr = ConfigTools.getPublicKey(keyPair);
        String privateKeyStr = ConfigTools.getPrivateKey(keyPair);
        System.out.println("publicKeyStr:" + publicKeyStr);
        System.out.println("privateKeyStr:" + privateKeyStr);
        String password = "123456";
        String byte2Base64 = ConfigTools.privateEncrypt(password, privateKeyStr);
        System.out.println("byte2Base64：" + byte2Base64);
        String pw = ConfigTools.publicDecrypt(byte2Base64, publicKeyStr);
        Assertions.assertEquals(pw, password);
    }
    
}
