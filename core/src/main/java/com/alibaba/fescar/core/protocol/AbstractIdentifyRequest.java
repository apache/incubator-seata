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

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract identify request.
 */
public abstract class AbstractIdentifyRequest extends AbstractMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIdentifyRequest.class);

    /**
     * The Version.
     */
    protected String version = Version.CURRENT;

    /**
     * The Application id.
     */
    protected String applicationId;

    /**
     * The Transaction service group.
     */
    protected String transactionServiceGroup;

    /**
     * The Extra data.
     */
    protected String extraData;

    /**
     * Instantiates a new Abstract identify request.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public AbstractIdentifyRequest(String applicationId, String transactionServiceGroup) {
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
    }

    /**
     * Instantiates a new Abstract identify request.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param extraData               the extra data
     */
    public AbstractIdentifyRequest(String applicationId, String transactionServiceGroup, String extraData) {
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
        this.extraData = extraData;
    }

    /**
     * Gets version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets version.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets application id.
     *
     * @return the application id
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets application id.
     *
     * @param applicationId the application id
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets transaction service group.
     *
     * @return the transaction service group
     */
    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    /**
     * Sets transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     */
    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    /**
     * Gets extra data.
     *
     * @return the extra data
     */
    public String getExtraData() {
        return extraData;
    }

    /**
     * Sets extra data.
     *
     * @param extraData the extra data
     */
    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    /**
     * The Byte buffer.
     */
    public ByteBuffer byteBuffer = ByteBuffer.allocate(10 * 1024);

    /**
     * Do encode.
     */
    protected void doEncode() {
        byteBuffer.clear();
        if (this.version != null) {
            byte[] bs = version.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

        if (this.applicationId != null) {
            byte[] bs = applicationId.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

        if (this.transactionServiceGroup != null) {
            byte[] bs = transactionServiceGroup.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

        if (this.extraData != null) {
            byte[] bs = extraData.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

    }

    private final byte[] flushEncode() {
        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        byteBuffer.clear(); // >?
        return content;
    }

    @Override
    public final byte[] encode() {
        doEncode();
        return flushEncode();
    }

    @Override
    public boolean decode(ByteBuf in) {
        try {
            short len = in.readShort();
            if (len > 0) {
                byte[] bs = new byte[len];
                in.readBytes(bs);
                this.setVersion(new String(bs, UTF8));
            }
            len = in.readShort();
            if (len > 0) {
                byte[] bs = new byte[len];
                in.readBytes(bs);
                this.setApplicationId(new String(bs, UTF8));
            }
            len = in.readShort();
            if (len > 0) {
                byte[] bs = new byte[len];
                in.readBytes(bs);
                this.setTransactionServiceGroup(new String(bs, UTF8));
            }
            len = in.readShort();
            if (len > 0) {
                byte[] bs = new byte[len];
                in.readBytes(bs);
                this.setExtraData(new String(bs, UTF8));
            }
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage() + this);
            return false;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(in.writerIndex() == in.readerIndex() ? "true" : "false" + this);
        }

        return true;
    }
}
