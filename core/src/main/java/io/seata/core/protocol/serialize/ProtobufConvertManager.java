/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.serialize;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.core.protocol.convertor.GlobalBeginRequestConvertor;
import io.seata.core.protocol.convertor.PbConvertor;
import io.seata.core.protocol.transaction.GlobalBeginRequest;

/**
 * @author bystander
 * @version : ProtobufConvertManager.java, v 0.1 2019年04月25日 07:41 bystander Exp $
 */
public class ProtobufConvertManager {

    private Map<String, PbConvertor> convertorMap = new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final ProtobufConvertManager INSTANCE;

        static {
            final ProtobufConvertManager protobufConvertManager = new ProtobufConvertManager();
            protobufConvertManager.convertorMap.put(GlobalBeginRequest.class.getName(),
                new GlobalBeginRequestConvertor());
            INSTANCE = protobufConvertManager;
        }

    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static final ProtobufConvertManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public PbConvertor fetcConvertor(String clazz) {
        return convertorMap.get(clazz);
    }
}