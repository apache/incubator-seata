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
// This file is originally from Apache SkyWalking
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
 *
 */

package seata.e2e.docker.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import seata.e2e.docker.extension.ContainerInitAndDestroyExtension;
import seata.e2e.model.HostAndPort;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Fields of type {@link HostAndPort} annotated with {@link ContainerHostAndPort @ContainerHostAndPort} can be
 * initialized by {@link ContainerInitAndDestroyExtension} with the real host and port of the docker container, whose original {@link
 * #name() service name} and {@link #port() exposed port}  defined in {@code docker-compose.yml} are given, for more
 * details and examples, refer to the JavaDoc of {@link ContainerInitAndDestroyExtension}.
 *
 * @author jingliu_xiong@foxmail.com
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface ContainerHostAndPort {
    /**
     * @return the original name that is defined in {@code docker-compose.yml}.
     */
    String name();

    /**
     * @return the original port that is exposed in {@code docker-compose.yml}.
     */
    int port();
}