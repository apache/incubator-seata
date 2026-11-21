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
package org.apache.seata.rm.datasource.xa;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Xid in XA Protocol. Wrap info of Seata xid and branchId.
 *
 */
public class XABranchXid implements XAXid {

    private static final String DEFAULT_ENCODE_CHARSET = "UTF-8";
    private static final String BRANCH_ID_PREFIX = "-";

    static final int SEATA_XA_XID_FORMAT_ID = 9752;

    /**
     * Format id for XA Xids whose gtrid is qualified with the branch id
     * (gtrid = xid + "-" + branchId), so that every branch of one Seata global transaction
     * owns a distinct gtrid. This is required by databases (e.g. DM) that reject a new
     * xa start with XAER_RMERR while an in-doubt branch of the same gtrid exists.
     * If the readable form would exceed the X/Open maximum gtrid length, a bounded
     * (md5-hex based) encoding of the xid is used instead.
     */
    public static final int SEATA_XA_XID_FORMAT_ID_PER_BRANCH_GTRID = 9753;

    /**
     * The maximum length of the global transaction id defined by the X/Open CAE Specification.
     */
    static final int MAX_GTRID_LENGTH = 64;

    private final int formatId;

    private String xid;

    private long branchId;

    private byte[] globalTransactionId;

    private byte[] branchQualifier;

    XABranchXid(String xid, long branchId) {
        this(xid, branchId, SEATA_XA_XID_FORMAT_ID);
    }

    /**
     * Build a XAXid with an explicit format id, which selects the gtrid encoding:
     * {@link #SEATA_XA_XID_FORMAT_ID_PER_BRANCH_GTRID} qualifies the gtrid with the branch id,
     * {@link #SEATA_XA_XID_FORMAT_ID} keeps the legacy gtrid (= xid).
     */
    XABranchXid(String xid, long branchId, int formatId) {
        this.xid = xid;
        this.branchId = branchId;
        this.formatId = formatId;
        encode();
    }

    /**
     * Rebuild a XAXid from raw XA bytes. Only the legacy encoding can be reconstructed this way;
     * per-branch-qualified gtrids must be rebuilt via {@link XAXidBuilder#build(String, long, String)}.
     */
    XABranchXid(byte[] globalTransactionId, byte[] branchQualifier) {
        this.globalTransactionId = globalTransactionId;
        this.branchQualifier = branchQualifier;
        this.formatId = SEATA_XA_XID_FORMAT_ID;
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
        return formatId;
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
        } else if (formatId == SEATA_XA_XID_FORMAT_ID_PER_BRANCH_GTRID) {
            globalTransactionId = encodeQualifiedGtrid();
        } else {
            globalTransactionId = string2byteArray(xid);
        }

        if (branchId == 0L) {
            branchQualifier = new byte[0];
        } else {
            branchQualifier = string2byteArray("-" + branchId);
        }
    }

    /**
     * Encode the per-branch-qualified gtrid as {@code xid + "-" + branchId}. When the readable
     * form would exceed the X/Open maximum gtrid length, fall back to the bounded form
     * {@code md5hex(xid) + "-" + branchId}, which always fits. Both forms are deterministic pure
     * functions of the xid and the branch id, so phase two rebuilds exactly the same bytes, and
     * distinct branch ids still own distinct gtrids.
     */
    private byte[] encodeQualifiedGtrid() {
        byte[] encodedGtrid = string2byteArray(xid + BRANCH_ID_PREFIX + branchId);
        if (encodedGtrid.length > MAX_GTRID_LENGTH) {
            return string2byteArray(md5Hex(string2byteArray(xid)) + BRANCH_ID_PREFIX + branchId);
        }
        return encodedGtrid;
    }

    private static String md5Hex(byte[] bytes) {
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(bytes);
            StringBuilder hex = new StringBuilder(md5.length * 2);
            for (byte b : md5) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // the MD5 algorithm is required to be available in every JRE
            throw new RuntimeException(e);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        XABranchXid that = (XABranchXid) o;
        return branchId == that.branchId && Objects.equals(xid, that.xid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xid, branchId);
    }
}
