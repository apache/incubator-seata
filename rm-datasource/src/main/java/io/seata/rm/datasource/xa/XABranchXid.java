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
package io.seata.rm.datasource.xa;

import java.io.UnsupportedEncodingException;

/**
 * Xid in XA Protocol. Wrap info of Seata xid and branchId.
 *
 * @author sharajava
 */
public class XABranchXid implements XAXid {

    private static final String DEFAULT_ENCODE_CHARSET = "UTF-8";
    private static final String BRANCH_ID_PREFIX = "-";

    private static final int SEATA_XA_XID_FORMAT_ID = 9752;

    private String xid;

    private long branchId;

    private byte[] globalTransactionId;

    private byte[] branchQualifier;

    XABranchXid(String xid, long branchId) {
        this.xid = xid;
        this.branchId = branchId;
        encode();
    }

    XABranchXid(byte[] globalTransactionId, byte[] branchQualifier) {
        this.globalTransactionId = globalTransactionId;
        this.branchQualifier = branchQualifier;
        decode();
    }

    @Override
    public String getGlobalXid() {
        return xid;
    }

    @Override
    public long getBranchId() {
        return branchId;
    }

    @Override
    public int getFormatId() {
        return SEATA_XA_XID_FORMAT_ID;
    }

    @Override
    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    @Override
    public byte[] getBranchQualifier() {
        return branchQualifier;
    }

    private byte[] string2byteArray(String string) {
        try {
            return string.getBytes(DEFAULT_ENCODE_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String byteArray2String(byte[] bytes) {
        try {
            return new String(bytes, DEFAULT_ENCODE_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void encode() {
        if (xid == null) {
            globalTransactionId = new byte[0];
        } else {
            globalTransactionId = string2byteArray(xid);
        }

        if (branchId == 0L) {
            branchQualifier = new byte[0];
        } else {
            branchQualifier = string2byteArray("-" + branchId);
        }
    }

    private void decode() {
        if (globalTransactionId == null || globalTransactionId.length == 0) {
            xid = null;
        } else {
            xid = byteArray2String(globalTransactionId);
        }


        if (branchQualifier == null || branchQualifier.length == 0) {
            branchId = 0L;
        } else {
            String bs = byteArray2String(branchQualifier).substring(BRANCH_ID_PREFIX.length());
            branchId = Long.parseLong(bs);
        }

    }

    @Override
    public String toString() {
        return xid + BRANCH_ID_PREFIX + branchId;
    }
}
