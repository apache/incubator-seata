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

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;

/**
 * @author goodboycoder
 */
public class BiStreamMessageTypeHelper {
    private static final Map<GrpcRemoting.BiStreamMessageType, Class<? extends Message>> MESSAGE_TYPE_CLASS_MAP = new HashMap<>();

    private static final Map<Class<? extends Message>, GrpcRemoting.BiStreamMessageType> STREAM_MESSAGE_TYPE_MAP = new HashMap<>();

    static {
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeBranchCommit, io.seata.serializer.protobuf.generated.BranchCommitRequestProto.class);
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeBranchCommitResult, io.seata.serializer.protobuf.generated.BranchCommitResponseProto.class);
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeBranchRollback, io.seata.serializer.protobuf.generated.BranchRollbackRequestProto.class);
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeBranchRollBackResult, io.seata.serializer.protobuf.generated.BranchRollbackResponseProto.class);
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TYPERegisterRMRequest, io.seata.serializer.protobuf.generated.RegisterRMRequestProto.class);
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TYPERegisterRMResponse, io.seata.serializer.protobuf.generated.RegisterRMResponseProto.class);
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TYPERegisterTMRequest, io.seata.serializer.protobuf.generated.RegisterTMRequestProto.class);
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TYPERegisterTMResponse, io.seata.serializer.protobuf.generated.RegisterTMResponseProto.class);
        MESSAGE_TYPE_CLASS_MAP.put(GrpcRemoting.BiStreamMessageType.TypeRMUndoLogDelete, io.seata.serializer.protobuf.generated.UndoLogDeleteRequestProto.class);

        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.BranchCommitRequestProto.class, GrpcRemoting.BiStreamMessageType.TypeBranchCommit);
        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.BranchCommitResponseProto.class, GrpcRemoting.BiStreamMessageType.TypeBranchCommitResult);
        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.BranchRollbackRequestProto.class, GrpcRemoting.BiStreamMessageType.TypeBranchRollback);
        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.BranchRollbackResponseProto.class, GrpcRemoting.BiStreamMessageType.TypeBranchRollBackResult);
        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.RegisterRMRequestProto.class, GrpcRemoting.BiStreamMessageType.TYPERegisterRMRequest);
        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.RegisterRMResponseProto.class, GrpcRemoting.BiStreamMessageType.TYPERegisterRMResponse);
        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.RegisterTMRequestProto.class, GrpcRemoting.BiStreamMessageType.TYPERegisterTMRequest);
        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.RegisterTMResponseProto.class, GrpcRemoting.BiStreamMessageType.TYPERegisterTMResponse);
        STREAM_MESSAGE_TYPE_MAP.put(io.seata.serializer.protobuf.generated.UndoLogDeleteRequestProto.class, GrpcRemoting.BiStreamMessageType.TypeRMUndoLogDelete);
    }

    public static Class<? extends Message> getBiStreamMessageClassType(GrpcRemoting.BiStreamMessageType messageType) {
        return MESSAGE_TYPE_CLASS_MAP.get(messageType);
    }

    public static GrpcRemoting.BiStreamMessageType getBiStreamMessageTypeByClass(Class<? extends Message> clazz) {
        return STREAM_MESSAGE_TYPE_MAP.get(clazz);
    }
}
