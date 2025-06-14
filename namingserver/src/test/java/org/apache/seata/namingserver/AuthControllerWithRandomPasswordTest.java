/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.namingserver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.seata.common.result.Code;
import org.apache.seata.console.config.WebSecurityConfig;
import org.apache.seata.console.security.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@AutoConfigureMockMvc
public class AuthControllerWithRandomPasswordTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void loginSuccess_shouldReturnTokenAndAddToHeader(CapturedOutput output) throws Exception {
        String logs = output.getOut();

        Pattern pattern = Pattern.compile("Use the auto-generated password: \\[(.+?)\\]");
        Matcher matcher = pattern.matcher(logs);

        assertTrue(matcher.find(), "captured password not found in logs");

        String extractedPassword = matcher.group(1);
        User user = new User("seata", extractedPassword);

        String userJson = objectMapper.writeValueAsString(user);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(header().exists(WebSecurityConfig.AUTHORIZATION_HEADER))
                .andReturn();

        String authHeader = result.getResponse().getHeader(WebSecurityConfig.AUTHORIZATION_HEADER);
        assertNotNull(authHeader);
        assert (authHeader.startsWith(WebSecurityConfig.TOKEN_PREFIX));
    }

    @Test
    public void loginFailure_shouldReturnErrorCode() throws Exception {
        User user = new User("wrong_user", "wrong_password");
        String userJson = objectMapper.writeValueAsString(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(Code.LOGIN_FAILED.getCode()));
    }
}
