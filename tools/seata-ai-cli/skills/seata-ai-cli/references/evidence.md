<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Investigation starting points

- **Cannot connect or find transactions:** separate Profile/trust/login problems from route discovery and management access. Check the intended namespace, cluster and vgroup before diagnosing an empty result as missing data. A reachable endpoint does not establish every capability.
- **Transaction stuck or retrying:** follow one XID through global state, branches and their RM/resource identities, then locks and the observation timeline. Compare repeated observations when necessary. Identify missing log or RM evidence explicitly; the current CLI does not collect these sources.
- **Lock contention:** relate locks to owning transactions/branches and resources. A lock row or shared resource alone does not establish an active waiter, deadlock or orphan. Check owner/session evidence and collection limits before recommending cleanup.
- **Integration/configuration mismatch:** compare local configuration with observed topology and connectivity. Project files and the CLI's environment do not establish effective TC/RM JVM configuration. Identify the unobserved configuration source rather than silently rewriting it.
- **Recovery or deletion:** gather fresh state and explain the intended target, effects and missing evidence for an operator handoff. The current CLI cannot execute these actions or dry-run plans. An empty management query does not establish completion or a safe cleanup target.
