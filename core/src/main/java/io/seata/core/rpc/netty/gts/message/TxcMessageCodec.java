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
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.*;
import io.seata.core.rpc.netty.gts.exception.TxcException;
import io.seata.core.serializer.Serializer;
import io.seata.core.serializer.SerializerServiceLoader;
import io.seata.core.serializer.SerializerType;

public class TxcMessageCodec {
    public static TxcCodec getTxcCodecInstance(short typeCode) {
        TxcCodec codec;
        switch (typeCode) {
            case 1:
                codec = new BeginMessage();
                break;
            case 2:
                codec = new BeginResultMessage();
                break;
            case 3:
                codec = new BranchCommitMessage();
                break;
            case 4:
                codec = new BranchCommitResultMessage();
                break;
            case 5:
                codec = new BranchRollbackMessage();
                break;
            case 6:
                codec = new BranchRollbackResultMessage();
                break;
            case 7:
                codec = new GlobalCommitMessage();
                break;
            case 8:
                codec = new GlobalCommitResultMessage();
                break;
            case 9:
                codec = new GlobalRollbackMessage();
                break;
            case 10:
                codec = new GlobalRollbackResultMessage();
                break;
            case 11:
                codec = new RegisterMessage();
                break;
            case 12:
                codec = new RegisterResultMessage();
                break;
            case 13:
                codec = new ReportStatusMessage();
                break;
            case 14:
                codec = new ReportStatusResultMessage();
                break;
            case 15:
                codec = new BeginRetryBranchMessage();
                break;
            case 16:
                codec = new BeginRetryBranchResultMessage();
                break;
            case 17:
                codec = new ReportUdataMessage();
                break;
            case 18:
                codec = new ReportUdataResultMessage();
                break;
            case 19:
                codec = new TxcMergeMessage();
                break;
            case 20:
                codec = new TxcMergeResultMessage();
                break;
            case 21:
                codec = new QueryLockMessage();
                break;
            case 22:
                codec = new QueryLockResultMessage();
                break;
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 101:
                codec = new RegisterClientAppNameMessage();
                break;
            case 102:
                codec = new RegisterClientAppNameResultMessage();
                break;
            case 103:
                codec = new RegisterRmMessage();
                break;
            case 104:
                codec = new RegisterRmResultMessage();
                break;
            case 113:
                codec = new ClusterDumpMessage();
                break;
            case 114:
                codec = new ClusterDumpResultMessage();
                break;
            case 121:
                codec = new RedressMessage();
                break;
            case 122:
                codec = new RedressResultMessage();
                break;
            default:
                String className = (String) TxcMessage.TYPE_MAP.get(typeCode);
                throw new TxcException("unknown class:" + className + " in txc message codec.");
        }
        return codec;
    }


    // TODO for every message type

    public static byte[] changetoSeataCodec(short typeCode, TxcCodec gtsCodec, ByteBuf out) {
        byte[] msgOut = null;
        switch (typeCode) {
            case 1: {
                GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
                BeginMessage beginMessage = (BeginMessage) gtsCodec;
                int timeout = (int) beginMessage.getTimeout();
                globalBeginRequest.setTimeout(timeout);
                String transactionName = beginMessage.getTxcInst();
                globalBeginRequest.setTransactionName(transactionName);
                // message type
                out.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
                // Serializer (default: seata)
                out.writeByte(1);
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(1));
                msgOut = serializer.serialize(globalBeginRequest);
                // Compress
                out.writeByte(0);
                return msgOut;
            }
            case 2: {
                GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
                BeginResultMessage beginResultMessage = (BeginResultMessage) gtsCodec;
                String xid = beginResultMessage.getXid();
                int result = beginResultMessage.getResult();
                globalBeginResponse.setXid(xid);
                globalBeginResponse.setResultCode(ResultCode.get(result));
                // message type
                out.writeByte(ProtocolConstants.MSGTYPE_RESPONSE);
                // Serializer (default: seata)
                out.writeByte(1);
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(1));
                msgOut = serializer.serialize(globalBeginResponse);
                // Compress
                out.writeByte(0);
                return msgOut;
            }
            case 3: {
                BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
                BranchCommitMessage branchCommitMessage = (BranchCommitMessage) gtsCodec;
                String serverAddr = branchCommitMessage.getServerAddr();
                String xid = serverAddr + ":" + String.valueOf(branchCommitMessage.getTranIds().get(0));
                Long branchId = branchCommitMessage.getBranchIds().get(0);
                String resourceId = branchCommitMessage.getDbName();
                String applicationData = branchCommitMessage.getUdata();
                branchCommitRequest.setXid(xid);
                branchCommitRequest.setBranchId(branchId);
                branchCommitRequest.setResourceId(resourceId);
                branchCommitRequest.setApplicationData(applicationData);
                branchCommitRequest.setBranchType(BranchType.GTS);
                // message type
                out.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
                // Serializer (default: seata)
                out.writeByte(1);
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(1));
                msgOut = serializer.serialize(branchCommitRequest);
                // Compress
                out.writeByte(0);
                return msgOut;
            }
            case 4: {
                BranchCommitResponse branchCommitResponse = new BranchCommitResponse();
                BranchCommitResultMessage branchCommitResultMessage = (BranchCommitResultMessage) gtsCodec;
                // still need to solve serverAddr
                String serverAddr = "127.0.0.1";
                String xid = serverAddr + ":" + String.valueOf(branchCommitResultMessage.getTranIds().get(0));
                Long branchId = branchCommitResultMessage.getBranchIds().get(0);
                branchCommitResponse.setXid(xid);
                branchCommitResponse.setBranchId(branchId);
                branchCommitResponse.setBranchStatus(BranchStatus.PhaseTwo_Committed);
                branchCommitResponse.setResultCode(ResultCode.get((byte) branchCommitResultMessage.getResult()));
                // message type
                out.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
                // Serializer (default: seata)
                out.writeByte(1);
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(1));
                msgOut = serializer.serialize(branchCommitResponse);
                // Compress
                out.writeByte(0);
                return msgOut;
            }
            case 5: {
                BranchRollbackRequest branchRollbackRequest = new BranchRollbackRequest();
                BranchRollbackMessage branchRollbackMessage = (BranchRollbackMessage) gtsCodec;
                String serverAddr = branchRollbackMessage.getServerAddr();
                String xid = serverAddr + ":" + String.valueOf(branchRollbackMessage.getTranId());
                long branchId = branchRollbackMessage.getBranchId();
                String resourceId = branchRollbackMessage.getDbName();
                String applicationData = branchRollbackMessage.getUdata();

                branchRollbackRequest.setXid(xid);
                branchRollbackRequest.setBranchId(branchId);
                branchRollbackRequest.setResourceId(resourceId);
                branchRollbackRequest.setApplicationData(applicationData);
                branchRollbackRequest.setBranchType(BranchType.GTS);

                // message type
                out.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
                // Serializer (default: seata)
                out.writeByte(1);
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(1));
                msgOut = serializer.serialize(branchRollbackRequest);
                // Compress
                out.writeByte(0);
                return msgOut;
            }
            default:
                return null;
        }
    }
}
