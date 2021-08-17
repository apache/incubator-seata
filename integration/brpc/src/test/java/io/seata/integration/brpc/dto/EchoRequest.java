package io.seata.integration.brpc.dto;

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

/**
 * @author mxz0828@163.com
 * @date 2021/8/16
 */
@ProtobufClass
public class EchoRequest {

    @Protobuf
    private String reqMsg;

    public String getReqMsg() {
        return reqMsg;
    }

    public void setReqMsg(String reqMsg) {
        this.reqMsg = reqMsg;
    }
}
