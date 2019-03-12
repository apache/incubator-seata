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

/**
 * The type Abstract result message.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/14
 */
public abstract class AbstractResultMessage extends AbstractMessage implements MergedMessage {
    private static final long serialVersionUID = 6540352050650203313L;

    private ResultCode resultCode;

    /**
     * The Byte buffer.
     */
    public ByteBuffer byteBuffer = ByteBuffer.allocate(512);

    /**
     * Gets result code.
     *
     * @return the result code
     */
    public ResultCode getResultCode() {
        return resultCode;
    }

    /**
     * Sets result code.
     *
     * @param resultCode the result code
     */
    public void setResultCode(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    private String msg;

    /**
     * Gets msg.
     *
     * @return the msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Sets msg.
     *
     * @param msg the msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * Do encode.
     */
    protected void doEncode() {
        byteBuffer.put((byte)resultCode.ordinal());
        if (resultCode == ResultCode.Failed) {
            if (getMsg() != null) {
                String msg;
                if (getMsg().length() > 128) {
                    msg = getMsg().substring(0, 128);
                } else {
                    msg = getMsg();
                }
                byte[] bs = msg.getBytes(UTF8);
                if (bs.length > 400 && getMsg().length() > 64) {
                    msg = getMsg().substring(0, 64);
                    bs = msg.getBytes(UTF8);
                }
                byteBuffer.putShort((short)bs.length);
                if (bs.length > 0) {
                    byteBuffer.put(bs);
                }
            } else {
                byteBuffer.putShort((short)0);
            }
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
    public void decode(ByteBuffer byteBuffer) {
        setResultCode(ResultCode.get(byteBuffer.get()));
        if (resultCode == ResultCode.Failed) {
            short len = byteBuffer.getShort();
            if (len > 0) {
                byte[] msg = new byte[len];
                byteBuffer.get(msg);
                this.setMsg(new String(msg, UTF8));
            }
        }
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 1) {
            return false;
        }
        setResultCode(ResultCode.get(in.readByte()));
        i--;
        if (resultCode == ResultCode.Failed) {
            if (i < 2) {
                return false;
            }
            short len = in.readShort();
            i -= 2;
            if (i < len) {
                return false;
            }

            if (len > 0) {
                byte[] msg = new byte[len];
                in.readBytes(msg);
                this.setMsg(new String(msg, UTF8));
            }
        }
        return true;
    }
}
