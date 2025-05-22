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
package org.apache.seata.console.security;

import java.util.UUID;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Custom user service
 *
 */
@Service
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUserDetailsServiceImpl.class);

    @Value("${console.user.username:seata}")
    private String username;

    @Value("${console.user.password:}")
    private String password;

    private User user;

    /**
     * Init.
     */
    @PostConstruct
    public void init() {
        if (!password.isEmpty()) {
            user = new User(username, new BCryptPasswordEncoder().encode(password));
            return;
        }

        password = generateRandomPassword();
        LOGGER.info(
                "No password was configured. A random password has been generated for security purposes. You may either:\n"
                        + "1. Use the auto-generated password: [{}]\n"
                        + "2. Set a custom password in the configuration.",
                password);

        user = new User(username, new BCryptPasswordEncoder().encode(password));
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        if (!user.getUsername().equals(userName)) {
            throw new UsernameNotFoundException(userName);
        }
        return new CustomUserDetails(user);
    }
}
