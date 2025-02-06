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
package org.apache.seata.server.limit.ratelimit;

import org.apache.seata.common.XID;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.NumberUtils;
import org.apache.seata.config.CachedConfigurationChangeListener;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.server.limit.AbstractTransactionRequestHandler;
import org.apache.seata.server.metrics.MetricsPublisher;

/**
 * RateLimiterHandler
 */
public class RateLimiterHandler extends AbstractTransactionRequestHandler implements CachedConfigurationChangeListener {
    /**
     * The instance of RateLimiterHandler
     */
    private static volatile RateLimiterHandler instance;

    /**
     * The instance of RateLimiter
     */
    private final RateLimiter rateLimiter;

    /**
     * The config of RateLimiterHandler
     */
    private final RateLimiterHandlerConfig config;

    public RateLimiterHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        this.config = new RateLimiterHandlerConfig();
    }

    private RateLimiterHandler() {
        rateLimiter = EnhancedServiceLoader.load(RateLimiter.class);
        config = rateLimiter.obtainConfig();

        Configuration config = ConfigurationFactory.getInstance();
        config.addConfigListener(ConfigurationKeys.RATE_LIMIT_ENABLE, this);
        config.addConfigListener(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_NUM_PER_SECOND, this);
        config.addConfigListener(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_MAX_NUM, this);
        config.addConfigListener(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_INITIAL_NUM, this);
    }

    @Override
    public AbstractTransactionResponse handle(AbstractTransactionRequestToTC originRequest, RpcContext context) {
        if (!rateLimiter.isEnable()) {
            return next(originRequest, context);
        }

        if (MessageType.TYPE_GLOBAL_BEGIN == originRequest.getTypeCode()) {
            if (!rateLimiter.canPass()) {
                GlobalBeginResponse response = new GlobalBeginResponse();
                response.setTransactionExceptionCode(TransactionExceptionCode.BeginFailed);
                response.setResultCode(ResultCode.Failed);
                RateLimitInfo rateLimitInfo = RateLimitInfo.generateRateLimitInfo(context.getApplicationId(),
                        RateLimitInfo.GLOBAL_BEGIN_FAILED, context.getClientId(), XID.getIpAddressAndPort());
                MetricsPublisher.postRateLimitEvent(rateLimitInfo);
                response.setMsg(String.format("TransactionException[rate limit exception, rate limit info: %s]", rateLimitInfo));
                return response;
            }
        }
        return next(originRequest, context);
    }

    public static RateLimiterHandler getInstance() {
        if (instance == null) {
            synchronized (RateLimiterHandler.class) {
                if (instance == null) {
                    instance = new RateLimiterHandler();
                }
            }
        }
        return instance;
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        String dataId = event.getDataId();
        String newValue = event.getNewValue();
        if (ConfigurationKeys.RATE_LIMIT_ENABLE.equals(dataId)) {
            config.setEnable(Boolean.parseBoolean(newValue));
        }
        if (ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_NUM_PER_SECOND.equals(dataId)) {
            config.setBucketTokenNumPerSecond(NumberUtils.toInt(newValue, config.getBucketTokenNumPerSecond()));
        }
        if (ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_MAX_NUM.equals(dataId)) {
            config.setBucketTokenMaxNum(NumberUtils.toInt(newValue, config.getBucketTokenMaxNum()));
        }
        if (ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_INITIAL_NUM.equals(dataId)) {
            config.setBucketTokenInitialNum(NumberUtils.toInt(newValue, config.getBucketTokenInitialNum()));
        }
        rateLimiter.reInit(config);
    }
}
