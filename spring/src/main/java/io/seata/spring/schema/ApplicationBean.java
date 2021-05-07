package io.seata.spring.schema;

import java.util.List;

/**
 * @author xingfudeshi@gmail.com
 * @date 2021/05/06
 */
public class ApplicationBean {
    private String applicationId;
    private String txServiceGroup;
    private String failureHandler;
    private int mode;
    private List<GtxBean> gtx;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }

    public String getFailureHandler() {
        return failureHandler;
    }

    public void setFailureHandler(String failureHandler) {
        this.failureHandler = failureHandler;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public List<GtxBean> getGtx() {
        return gtx;
    }

    public void setGtx(List<GtxBean> gtx) {
        this.gtx = gtx;
    }

    @Override
    public String toString() {
        return "ApplicationBean{" +
            "applicationId='" + applicationId + '\'' +
            ", txServiceGroup='" + txServiceGroup + '\'' +
            ", failureHandler='" + failureHandler + '\'' +
            ", mode=" + mode +
            ", gtx=" + gtx +
            '}';
    }
}
