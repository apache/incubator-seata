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
package io.seata.core.rpc.grpc;

import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.netty.shaded.io.netty.channel.Channel;

/**
 * @author goodboycoder
 */
public class ContextKeyConstants {
    public static final Attributes.Key<String> CONNECT_ID = Attributes.Key.create("connect_id");

    public static final Context.Key<String> CUR_CONNECT_ID = Context.key("connect_id");
    public static final Context.Key<Channel> CUR_CONNECTION = Context.key("cur_connection");
}
