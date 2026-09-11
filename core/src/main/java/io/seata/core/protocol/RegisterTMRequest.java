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
package io.seata.core.protocol;

import java.io.Serializable;

import io.seata.common.util.NetUtil;
import org.apache.commons.lang.StringUtils;

import static io.seata.core.constants.ConfigurationKeys.EXTRA_DATA_SPLIT_CHAR;

/**
 * The type Register tm request.
 *
 * @author slievrly
 */
public class RegisterTMRequest extends AbstractIdentifyRequest implements Serializable {
    private static final long serialVersionUID = -5929081344190543690L;
    public static final String UDATA_VGROUP = "vgroup";
    public static final String UDATA_AK = "ak";
    public static final String UDATA_DIGEST = "digest";
    public static final String UDATA_IP = "ip";
    public static final String UDATA_TIMESTAMP = "timestamp";
    public static final String UDATA_AUTH_VERSION = "authVersion";

    /**
     * Instantiates a new Register tm request.
     */
    public RegisterTMRequest() {
        this(null, null);
    }

    /**
     * Instantiates a new Register tm request.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param extraData               the extra data
     */
    public RegisterTMRequest(String applicationId, String transactionServiceGroup, String extraData) {
        super(applicationId, transactionServiceGroup, extraData);
        StringBuilder sb = new StringBuilder();
        if (null != extraData) {
            sb.append(extraData);
            if (!extraData.endsWith(EXTRA_DATA_SPLIT_CHAR)) {
                sb.append(EXTRA_DATA_SPLIT_CHAR);
            }
        }
        if (transactionServiceGroup != null && !transactionServiceGroup.isEmpty()) {
            sb.append(String.format("%s=%s", UDATA_VGROUP, transactionServiceGroup));
            sb.append(EXTRA_DATA_SPLIT_CHAR);
            String clientIP = NetUtil.getLocalIp();
            if (!StringUtils.isEmpty(clientIP)) {
                sb.append(String.format("%s=%s", UDATA_IP, clientIP));
                sb.append(EXTRA_DATA_SPLIT_CHAR);
            }
        }
        this.extraData = sb.toString();

    }

    /**
     * Instantiates a new Register tm request.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public RegisterTMRequest(String applicationId, String transactionServiceGroup) {
        this(applicationId, transactionServiceGroup, null);
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_REG_CLT;
    }

}
