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

package seata.e2e.docker.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import seata.e2e.docker.extension.Envs;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/**
 * A class to load YAML content as a type-safe object.
 */
public final class Yamls {
    public interface Builder {
    }

    public interface AsTypeBuilder extends Builder {
        <T> T as(final Class<T> klass);
    }


    public static AsTypeBuilder load(final String file) throws IOException {
        final InputStream inputStream = new ClassPathResource(Envs.resolve(file)).getInputStream();

        return new AsTypeBuilder() {
            @Override
            public <T> T as(final Class<T> klass) {
                return new Yaml().loadAs(inputStream, klass);
            }
        };
    }

    public static AsTypeBuilder load(final File file) throws IOException {
        final InputStream inputStream = new FileInputStream(file);

        return new AsTypeBuilder() {
            @Override
            public <T> T as(final Class<T> klass) {
                return new Yaml().loadAs(inputStream, klass);
            }
        };
    }

    public static AsTypeBuilder load(final StringBuilder content) {
        return new AsTypeBuilder() {
            @Override
            public <T> T as(final Class<T> klass) {
                return new Yaml().loadAs(content.toString(), klass);
            }
        };
    }
}