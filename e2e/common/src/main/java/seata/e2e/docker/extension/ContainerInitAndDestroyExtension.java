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

package seata.e2e.docker.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import seata.e2e.docker.E2E;
import seata.e2e.docker.annotation.ContainerHostAndPort;
import seata.e2e.docker.annotation.DockerCompose;

/**
 * This annotation supports the {@link DockerCompose @DockerCompose} and {@link ContainerHostAndPort @ContainerHostAndPort} annotations.
 * You can use {@link E2E} to instead of this annotation, too.
 *
 * <pre>{@code
 * @ExtendWith(ContainerInitAndDestroyExtension.class)
 * @TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * public class SomeTest {
 *     @DockerCompose("docker-compose.yml")
 *     private DockerComposeContainer consumer;
 *
 *     @ContainerHostAndPort(name = "service-name1-in-docker-compose.yml", port = 8080)
 *     private HostAndPort someService1HostPort;
 *
 *     @ContainerHostAndPort(name = "service-name2-in-docker-compose.yml", port = 9090)
 *     private HostAndPort someService2HostPort;
 * }
 * }</pre>
 *
 * @author jingliu_xiong@foxmail.com
 */
public class ContainerInitAndDestroyExtension implements BeforeAllCallback, AfterAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // context.getRequiredTestInstance() get the actual running object
        ContainerInitAndDestroy.init(context.getRequiredTestInstance());
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ContainerInitAndDestroy.destroy(context.getRequiredTestInstance());
    }
}