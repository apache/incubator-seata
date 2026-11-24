<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Building Seata

Run commands from the repository root with `JAVA_HOME` pointing to the
intended JDK. Use `./mvnw` (`mvnw.cmd` on Windows) so that Maven matches the
version pinned in `.mvn/wrapper/maven-wrapper.properties`.

## Development build

```shell
./mvnw -version
./mvnw clean test
```

The root reactor selects additional modules according to the JDK running
Maven. These are build prerequisites, not a promise that all resulting
artifacts have the same minimum runtime version.

| JDK running Maven | Additional modules selected by JDK profiles |
| --- | --- |
| 8 | Base client/library reactor; no Server, Console, Naming Server or Loom module |
| 17 | Server and the new-version test suite |
| 21 | The JDK 17 modules plus `threadpool-loom` |
| 25 | The JDK 21 modules plus Console and Naming Server |

A successful JDK 8 or 17 build therefore does not validate the complete
distribution. The CI matrix tests JDK 8, 17, 21 and 25. Redis and Nacos
integration tests are opt-in locally; the CI service configuration and
`redisCaseEnabled` / `nacosCaseEnabled` flags are in `.github/workflows/build.yml`.

## Complete distribution

Use JDK 25 or later for the complete distribution, which includes the
Console and Naming Server. The root `release-seata` profile rejects older
JDKs during `validate`, before packaging can silently omit these modules.

```shell
./mvnw -version
./mvnw -T 4 clean install -Prelease-seata -DskipTests
```

This packaging command compiles tests but skips their execution. Run the
development tests separately when validating a change. The Console build
installs the Node version pinned in `build/pom.xml`; dependency downloads
require network access on a clean machine.

Check the resulting application JARs and distribution before using them:

- `server/target/seata-server-exec.jar`
- `namingserver/target/seata-namingserver-exec.jar`
- Source and binary archives under `distribution/target/`

The `release-seata-jar` profile is a separate application packaging mode;
the complete-distribution JDK check does not change the JDK requirements
of individual modules or the default client build.
