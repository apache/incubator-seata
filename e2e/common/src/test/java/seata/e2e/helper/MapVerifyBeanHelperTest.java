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

package seata.e2e.helper;

import java.util.HashMap;
import java.util.Map;

import seata.e2e.model.HostAndPort;
import org.junit.Test;

/**
 * @author jingliu_xiong@foxmail.com
 */
public class MapVerifyBeanHelperTest {

    @Test
    public void mapAllEqualBeanTest() {
        Map<String, Object> map = new HashMap<>();
        map.put("host", "localhost");
        map.put("port", "3306");
        HostAndPort localhost = new HostAndPort("localhost", 3306);
        MapVerifyBeanHelper mapVerifyBeanHelper = new MapVerifyBeanHelper(map, localhost);
        boolean b = mapVerifyBeanHelper.mapAllEqualBeanSome();
        System.out.println(b);
        boolean b1 = mapVerifyBeanHelper.mapOneEqualBeanOne("host");
        System.out.println(b1);
    }

}
