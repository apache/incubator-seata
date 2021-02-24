package io.seata.rm.tcc;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.config.TCCFenceConfig;
import io.seata.rm.tcc.constant.TCCFenceConstant;
import io.seata.rm.tcc.exception.TCCFenceException;
import io.seata.rm.tcc.store.TCCFenceDO;
import io.seata.rm.tcc.store.TCCFenceStore;
import io.seata.rm.tcc.store.db.TCCFenceStoreDataBaseDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;

/**
 * TCC Fence Handler(idempotent, non_rollback, suspend)
 *
 * @author cebbank
 */
public class TCCFenceHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCFenceHandler.class);

    private static TCCFenceStore tccFenceDAO = TCCFenceStoreDataBaseDAO.getInstance();

    /**
     * tcc prepare method enhanced
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static boolean prepareFence(String xid, Long branchId) {
        DataSource dataSource = TCCFenceConfig.getDataSource();
        if (dataSource == null) {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
        TCCFenceDO tccFenceDO = new TCCFenceDO();
        tccFenceDO.setXid(xid);
        tccFenceDO.setBranchId(branchId);
        tccFenceDO.setStatus(TCCFenceConstant.STATUS_TRIED);
        boolean result = tccFenceDAO.insertTCCFenceDO(dataSource, tccFenceDO);
        if (result) {
            LOGGER.info("TCC fence prepare result: true. xid: {}, branchId: {}", xid, branchId);
            return true;
        } else {
            LOGGER.warn("Insert tcc fence record error, prepare fence failed. xid: {}, branchId: {}", xid, branchId);
            throw new TCCFenceException(String.format("Insert tcc fence record error, prepare fence failed. xid= %s, branchId= %s", xid, branchId),
                    FrameworkErrorCode.InsertRecordError);
        }
    }

    /**
     * tcc commit method enhanced
     * @param commitMethod commit method
     * @param targetTCCBean target tcc bean
     * @param businessActionContext businessActionContext
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static boolean commitFence(Method commitMethod, Object targetTCCBean, BusinessActionContext businessActionContext, String xid, Long branchId) {
        DataSource dataSource = TCCFenceConfig.getDataSource();
        if (dataSource == null) {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
        TCCFenceDO tccFenceDO = tccFenceDAO.queryTCCFenceDO(dataSource, xid, branchId);
        if (tccFenceDO == null) {
            throw new TCCFenceException(String.format("TCC fence record not exists, commit fence method failed. xid= %s, branchId= %s", xid, branchId),
                    FrameworkErrorCode.RecordAlreadyExists);
        }
        if (TCCFenceConstant.STATUS_COMMITTED == tccFenceDO.getStatus()) {
            LOGGER.info("Branch transaction has already committed before. idempotency rejected. xid: {}, branchId: {}", xid, branchId);
            return true;
        }
        if (TCCFenceConstant.STATUS_ROLLBACKED == tccFenceDO.getStatus() || TCCFenceConstant.STATUS_SUSPENDED == tccFenceDO.getStatus()) {
            LOGGER.warn("Branch transaction status is unexpected. xid: {}, branchId: {}", xid, branchId);
            return false;
        }
        return updateStatusAndInvokeTargetMethod(dataSource, commitMethod, targetTCCBean, businessActionContext, xid, branchId, TCCFenceConstant.STATUS_COMMITTED);
    }

    /**
     * tcc rollback method enhanced
     * @param rollbackMethod rollback method
     * @param targetTCCBean target tcc bean
     * @param businessActionContext businessActionContext
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static boolean rollbackFence(Method rollbackMethod, Object targetTCCBean, BusinessActionContext businessActionContext, String xid, Long branchId) {
        DataSource dataSource = TCCFenceConfig.getDataSource();
        if (dataSource == null) {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
        TCCFenceDO tccFenceDO = tccFenceDAO.queryTCCFenceDO(dataSource, xid, branchId);
        // non_rollback
        if (tccFenceDO == null) {
            tccFenceDO = new TCCFenceDO();
            tccFenceDO.setXid(xid);
            tccFenceDO.setBranchId(branchId);
            tccFenceDO.setStatus(TCCFenceConstant.STATUS_SUSPENDED);
            boolean result = tccFenceDAO.insertTCCFenceDO(dataSource, tccFenceDO);
            if (!result) {
                LOGGER.warn("Insert tcc fence record error, rollback fence method failed. xid: {}, branchId: {}", xid, branchId);
                return false;
            }
        } else {
            if (TCCFenceConstant.STATUS_COMMITTED == tccFenceDO.getStatus()) {
                LOGGER.info("Branch transaction status is unexpected. xid: {}, branchId: {}", xid, branchId);
                return false;
            }
            if (TCCFenceConstant.STATUS_ROLLBACKED == tccFenceDO.getStatus()) {
                LOGGER.info("Branch transaction had already rollbacked before, idempotency rejected. xid: {}, branchId: {}", xid, branchId);
                return true;
            }
        }
        return updateStatusAndInvokeTargetMethod(dataSource, rollbackMethod, targetTCCBean, businessActionContext, xid, branchId, TCCFenceConstant.STATUS_ROLLBACKED);
    }

    /**
     * Update TCC Fence status and invoke target method
     * @param method target method
     * @param targetTCCBean target bean
     * @param businessActionContext businessActionContext
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @param status the tcc fence status
     * @return the boolean
     */
    private static boolean updateStatusAndInvokeTargetMethod(DataSource dataSource, Method method, Object targetTCCBean, BusinessActionContext businessActionContext, String xid, Long branchId, int status) {
        try {
            boolean result = tccFenceDAO.updateTCCFenceDO(dataSource, xid, branchId, status, TCCFenceConstant.STATUS_TRIED);
            if (result) {
                // invoke two phase method
                Object ret = method.invoke(targetTCCBean, businessActionContext);
                if (null != ret) {
                    if (ret instanceof TwoPhaseResult) {
                        result = ((TwoPhaseResult) ret).isSuccess();
                    }else {
                        result = (boolean) ret;
                    }
                }
            }
            if (!result) {
                // rollback tcc fence log status
                tccFenceDAO.updateTCCFenceDO(dataSource, xid, branchId, TCCFenceConstant.STATUS_TRIED, status);
            }
            return result;
        } catch (Throwable t) {
            throw new TCCFenceException(t);
        }
    }

    /**
     * Delete TCC Fence
     * @param xid the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static boolean deleteFence(String xid, Long branchId) {
        DataSource dataSource = TCCFenceConfig.getDataSource();
        if (dataSource == null) {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
        return tccFenceDAO.deleteTCCFenceDO(dataSource, xid, branchId);
    }

    /**
     * Delete TCC Fence By Datetime
     * @param datetime datetime
     * @return the boolean
     */
    public static boolean deleteFenceByDate(Date datetime) {
        DataSource dataSource = TCCFenceConfig.getDataSource();
        if (dataSource == null) {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
        return tccFenceDAO.deleteTCCFenceDOByDate(dataSource, new Timestamp(datetime.getTime()));
    }
}
