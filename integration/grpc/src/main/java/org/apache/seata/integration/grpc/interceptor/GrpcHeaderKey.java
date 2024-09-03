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
package org.apache.seata.integration.grpc.interceptor;

import io.grpc.Metadata;
import org.apache.seata.core.context.RootContext;


public interface GrpcHeaderKey {

    Metadata.Key<String> XID_HEADER_KEY = Metadata.Key.of(RootContext.KEY_XID, Metadata.ASCII_STRING_MARSHALLER);

    Metadata.Key<String> XID_HEADER_KEY_LOWERCASE = Metadata.Key.of(RootContext.KEY_XID.toLowerCase(), Metadata.ASCII_STRING_MARSHALLER);

    Metadata.Key<String> BRANCH_HEADER_KEY = Metadata.Key.of(RootContext.KEY_BRANCH_TYPE, Metadata.ASCII_STRING_MARSHALLER);
}
