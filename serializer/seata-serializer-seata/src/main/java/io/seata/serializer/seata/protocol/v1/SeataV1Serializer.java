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
package io.seata.serializer.seata.protocol.v1;

import com.sun.tools.javac.util.Pair;
import io.seata.common.loader.LoadLevel;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.serializer.seata.SeataAbstractSerializer;

/**
 * The Seata codec v1.
 *
 * @author Bughue
 */
@LoadLevel(name = "SEATA", version = ProtocolConstants.VERSION_1)
public class SeataV1Serializer extends SeataAbstractSerializer {

    public SeataV1Serializer() {
        classMap.put(MessageType.TYPE_REG_CLT, new Pair<>(RegisterTMRequestCodec.class, RegisterTMRequest.class));
        classMap.put(MessageType.TYPE_REG_CLT_RESULT, new Pair<>(RegisterTMResponseCodec.class, RegisterTMResponse.class));
        classMap.put(MessageType.TYPE_REG_RM, new Pair<>(RegisterRMRequestCodec.class, RegisterRMRequest.class));
        classMap.put(MessageType.TYPE_REG_RM_RESULT, new Pair<>(RegisterRMResponseCodec.class, RegisterRMResponse.class));

    }




}
