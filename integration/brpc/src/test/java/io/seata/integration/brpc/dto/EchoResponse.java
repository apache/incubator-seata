package io.seata.integration.brpc.dto;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

/**
 * @author mxz0828@163.com
 * @date 2021/8/16
 */
@ProtobufClass
public class EchoResponse {

    @Protobuf
    private String xid;

    @Protobuf
    private String branchType;

    @Protobuf
    private String reqMsg;

    public EchoResponse(String xid, String branchType,String reqMsg) {
        this.xid = xid;
        this.branchType = branchType;
        this.reqMsg = reqMsg;
    }

    public EchoResponse() {
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public void setBranchType(String branchType) {
        this.branchType = branchType;
    }

    public void setReqMsg(String reqMsg) {
        this.reqMsg = reqMsg;
    }

    public String getXid() {
        return xid;
    }

    public String getBranchType() {
        return branchType;
    }

    public String getReqMsg() {
        return reqMsg;
    }
}
