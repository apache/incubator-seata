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
package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.io.Serializable;
import java.nio.ByteBuffer;
import io.seata.common.util.NetUtil;
// TODO transform version between gts and seata
import io.seata.core.protocol.Version;
import org.apache.commons.lang.StringUtils;

public class RegisterClientAppNameMessage extends TxcMessage implements Serializable {
    private static final long serialVersionUID = -5929081344190543690L;
    private String clientAppName;
    private String version;
    private String txcInsts;
    private String udata;
    private String vgroupName;
    private int clientType;
    private String udataIp;
    public static final String UDATA_VGROUP = "vgroup";
    public static final String UDATA_AK = "ak";
    public static final String UDATA_DIGEST = "digest";
    public static final String UDATA_TYPE = "type";
    public static final String UDATA_IP = "ip";
    public static final String UDATA_TIMESTAMP = "timestamp";
    public ByteBuffer byteBuffer;

    public RegisterClientAppNameMessage() {
        this.version = Version.getCurrent();
        this.udata = "";
        this.byteBuffer = ByteBuffer.allocate(2097152);
    }

    public RegisterClientAppNameMessage(String appName) {
        this(appName, (String)null, appName, 0);
    }

    public RegisterClientAppNameMessage(String appName, String txcInsts, String vgroupName, int clientType) {
        this.version = Version.getCurrent();
        this.udata = "";
        this.byteBuffer = ByteBuffer.allocate(2097152);
        this.clientAppName = appName;
        this.txcInsts = txcInsts;
        this.vgroupName = vgroupName;
        this.clientType = clientType;
        if (vgroupName != null && !vgroupName.isEmpty()) {
            this.udata = String.format("%s=%s\n", "vgroup", vgroupName);
            this.udataIp = NetUtil.getLocalIp();
            if (!StringUtils.isEmpty(this.udataIp)) {
                this.udata = String.format("%s\n%s=%s\n", this.udata, "ip", this.udataIp);
            }
        }

    }

    public String getClientAppName() {
        return this.clientAppName;
    }

    public void setClientAppName(String clientAppName) {
        this.clientAppName = clientAppName;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setTxcInsts(String txcInsts) {
        this.txcInsts = txcInsts;
    }

    public String getTxcInsts() {
        return this.txcInsts;
    }

    @Override
    public short getTypeCode() {
        return 101;
    }

    @Override
    public byte[] encode() {
        byte[] bs;
        if (this.clientAppName != null) {
            bs = this.clientAppName.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.version != null) {
            bs = this.version.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.txcInsts != null) {
            bs = this.txcInsts.getBytes(UTF8);
            this.byteBuffer.putInt(bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putInt(0);
        }

        if (this.udata != null && !this.udata.isEmpty()) {
            bs = this.udata.getBytes(UTF8);
            this.byteBuffer.putInt(bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putInt(0);
        }

        this.byteBuffer.flip();
        bs = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(bs);
        return bs;
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 8) {
            return false;
        } else {
            i -= 8;
            short len = in.readShort();
            byte[] bs;
            if (len > 0) {
                if (i < len) {
                    return false;
                }

                i -= len;
                bs = new byte[len];
                in.readBytes(bs);
                this.setClientAppName(new String(bs, UTF8));
            }

            len = in.readShort();
            if (len > 0) {
                if (i < len) {
                    return false;
                }

                i -= len;
                bs = new byte[len];
                in.readBytes(bs);
                this.setVersion(new String(bs, UTF8));
            }

            int ilen = in.readInt();
            if (ilen > 0) {
                if (i < ilen) {
                    return false;
                }

                i -= ilen;
                bs = new byte[ilen];
                in.readBytes(bs);
                this.setTxcInsts(new String(bs, UTF8));
            }

            if (i < 4) {
                return false;
            } else {
                ilen = in.readInt();
                i -= 4;
                if (ilen > 0) {
                    if (i < ilen) {
                        return false;
                    }

                    bs = new byte[ilen];
                    in.readBytes(bs);
                    this.setUdata(new String(bs, UTF8));
                }

                return true;
            }
        }
    }

    public String getUdata() {
        return this.udata;
    }

    public void setUdata(String udata) {
        this.udata = udata;
    }

}
