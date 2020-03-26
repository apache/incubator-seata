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
package io.seata.core.serializer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;

/**
 * Provide a unified serialization registry, this class used for {@code seata-serializer-fst}
 * and {@code seata-serializer-kryo}, it will register some classes at startup time (for example {@link KryoSerializerFactory#create})
 * @author funkye
 */
public class SerializerClassRegistry {

    private static final Map<Class<?>, Object> REGISTRATIONS = new LinkedHashMap<>();

    static {

        // register commonly class
        registerClass(HashMap.class);
        registerClass(ArrayList.class);
        registerClass(LinkedList.class);
        registerClass(HashSet.class);
        registerClass(TreeSet.class);
        registerClass(Hashtable.class);
        registerClass(Date.class);
        registerClass(Calendar.class);
        registerClass(ConcurrentHashMap.class);
        registerClass(SimpleDateFormat.class);
        registerClass(GregorianCalendar.class);
        registerClass(Vector.class);
        registerClass(BitSet.class);
        registerClass(StringBuffer.class);
        registerClass(StringBuilder.class);
        registerClass(Object.class);
        registerClass(Object[].class);
        registerClass(String[].class);
        registerClass(byte[].class);
        registerClass(char[].class);
        registerClass(int[].class);
        registerClass(float[].class);
        registerClass(double[].class);

        // register seata protocol relation class
        registerClass(BranchCommitRequest.class);
        registerClass(BranchCommitResponse.class);
        registerClass(BranchRegisterRequest.class);
        registerClass(BranchRegisterResponse.class);
        registerClass(BranchReportRequest.class);
        registerClass(BranchReportResponse.class);
        registerClass(BranchRollbackRequest.class);
        registerClass(BranchRollbackResponse.class);
        registerClass(GlobalBeginRequest.class);
        registerClass(GlobalBeginResponse.class);
        registerClass(GlobalCommitRequest.class);
        registerClass(GlobalCommitResponse.class);
        registerClass(GlobalLockQueryRequest.class);
        registerClass(GlobalLockQueryResponse.class);
        registerClass(GlobalRollbackRequest.class);
        registerClass(GlobalRollbackResponse.class);
        registerClass(GlobalStatusRequest.class);
        registerClass(GlobalStatusResponse.class);
        registerClass(UndoLogDeleteRequest.class);
        registerClass(GlobalReportRequest.class);
        registerClass(GlobalReportResponse.class);

        registerClass(MergedWarpMessage.class);
        registerClass(MergeResultMessage.class);
        registerClass(RegisterRMRequest.class);
        registerClass(RegisterRMResponse.class);
        registerClass(RegisterTMRequest.class);
        registerClass(RegisterTMResponse.class);
    }
    
    /**
     * only supposed to be called at startup time
     *
     * @param clazz object type
     */
    public static void registerClass(Class<?> clazz) {
        registerClass(clazz, null);
    }

    /**
     * only supposed to be called at startup time
     *
     * @param clazz object type
     * @param serializer object serializer
     */
    public static void registerClass(Class<?> clazz, Object serializer) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class registered cannot be null!");
        }
        REGISTRATIONS.put(clazz, serializer);
    }

    /**
     * get registered classes
     *
     * @return class serializer
     * */
    public static Map<Class<?>, Object> getRegisteredClasses() {
        return REGISTRATIONS;
    }
}
