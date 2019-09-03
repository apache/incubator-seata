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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import io.seata.saga.statelang.domain.StateMachine;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * StateParser tests
 * @author lorne.cl
 */
public class StateParserTests {

    @Test
    public void testParser() throws IOException {

        ClassPathResource resource = new ClassPathResource("statelang/simple_statemachine.json");
        String json = IOUtils.toString(resource.getInputStream(), "UTF-8");
        StateMachine stateMachine = StateMachineParserFactory.getStateMachineParser().parse(json);
        Assert.assertNotNull(stateMachine);

        String outputJson = (new GsonBuilder()).
                setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).
                disableHtmlEscaping().
                setPrettyPrinting().create().
                toJson(stateMachine);
        System.out.println(outputJson);

        Assert.assertEquals(stateMachine.getName(), "simpleTestStateMachine");
        Assert.assertTrue(stateMachine.getStates().size() > 0);
    }
}