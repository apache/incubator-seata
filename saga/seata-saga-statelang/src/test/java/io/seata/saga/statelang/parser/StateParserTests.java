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
package io.seata.saga.statelang.parser;

import java.io.IOException;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.parser.utils.DesignerJsonTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * StateParser tests
 *
 * @author lorne.cl
 */
public class StateParserTests {

    @Test
    public void testParser() throws IOException {

        ClassPathResource resource = new ClassPathResource("statelang/simple_statemachine.json");
        String json = io.seata.saga.statelang.parser.utils.IOUtils.toString(resource.getInputStream(), "UTF-8");
        StateMachine stateMachine = StateMachineParserFactory.getStateMachineParser().parse(json);
        Assertions.assertNotNull(stateMachine);

        String outputJson = JSON.toJSONString(stateMachine, SerializerFeature.PrettyFormat);
        System.out.println(outputJson);

        Assertions.assertEquals(stateMachine.getName(), "simpleTestStateMachine");
        Assertions.assertTrue(stateMachine.getStates().size() > 0);
    }

    @Test
    public void testDesignerJsonTransformer() throws IOException {

        ClassPathResource resource = new ClassPathResource("statelang/simple_statemachine_with_layout.json");
        String json = io.seata.saga.statelang.parser.utils.IOUtils.toString(resource.getInputStream(), "UTF-8");
        Map<String, Object> parsedObj = DesignerJsonTransformer.toStandardJson(JSON.parseObject(json, Feature.OrderedField));
        Assertions.assertNotNull(parsedObj);

        String outputJson = JSON.toJSONString(parsedObj, SerializerFeature.PrettyFormat);
        System.out.println(outputJson);
    }
}