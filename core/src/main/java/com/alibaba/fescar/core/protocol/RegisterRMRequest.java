/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.protocol;

import java.io.Serializable;

import io.netty.buffer.ByteBuf;

/**
 * The type Register rm request.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /10/10 14:43
 * @FileName: RegisterRMRequest
 * @Description:
 */
public class RegisterRMRequest extends AbstractIdentifyRequest implements Serializable {
    private static final long serialVersionUID = 7539732523682335742L;

    private String resourceIds;

    /**
     * Instantiates a new Register rm request.
     */
    public RegisterRMRequest() {
        this(null, null);
    }

    /**
     * Instantiates a new Register rm request.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public RegisterRMRequest(String applicationId, String transactionServiceGroup) {
        super(applicationId, transactionServiceGroup);
    }

    /**
     * Gets resource ids.
     *
     * @return the resource ids
     */
    public String getResourceIds() {
        return resourceIds;
    }

    /**
     * Sets resource ids.
     *
     * @param resourceIds the resource ids
     */
    public void setResourceIds(String resourceIds) {
        this.resourceIds = resourceIds;
    }
    @Override
    public short getTypeCode() {
        return TYPE_REG_RM;
    }

    @Override
    protected void doEncode() {
        super.doEncode();
        if (this.resourceIds != null) {
            byte[] bs = resourceIds.getBytes(UTF8);
            byteBuffer.putInt(bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putInt(0);
        }
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 1) {
            return false;
        }

        short len = in.readShort();
        if (len > 0) {
            if (i < len) {
                return false;
            } else {
                i -= len;
            }
            byte[] bs = new byte[len];
            in.readBytes(bs);
            this.setVersion(new String(bs, UTF8));
        }

        len = in.readShort();
        if (len > 0) {
            if (i < len) {
                return false;
            } else {
                i -= len;
            }
            byte[] bs = new byte[len];
            in.readBytes(bs);
            this.setApplicationId(new String(bs, UTF8));
        }

        len = in.readShort();
        if (len > 0) {
            if (i < len) {
                return false;
            } else {
                i -= len;
            }
            byte[] bs = new byte[len];
            in.readBytes(bs);
            this.setTransactionServiceGroup(new String(bs, UTF8));
        }

        len = in.readShort();
        if (len > 0) {
            if (i < len) {
                return false;
            } else {
                i -= len;
            }
            byte[] bs = new byte[len];
            in.readBytes(bs);
            this.setExtraData(new String(bs, UTF8));
        }

        int iLen = in.readInt();
        if (iLen > 0) {
            if (i < iLen) {
                return false;
            } else {
                i -= iLen;
            }
            byte[] bs = new byte[iLen];
            in.readBytes(bs);
            this.setResourceIds(new String(bs, UTF8));
        }
        return true;
    }

    @Override
    public String toString() {
        return "RegisterRMRequest{" +
            "resourceIds='" + resourceIds + '\'' +
            ", applicationId='" + applicationId + '\'' +
            ", transactionServiceGroup='" + transactionServiceGroup + '\'' +
            '}';
    }
}
