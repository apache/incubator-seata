package com.alibaba.fescar.server.session.db;

import com.alibaba.fescar.common.loader.LoadLevel;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.server.session.BranchSession;
import com.alibaba.fescar.server.session.GlobalSession;
import com.alibaba.fescar.server.session.SessionCondition;
import com.alibaba.fescar.server.session.SessionLifecycleListener;
import com.alibaba.fescar.server.session.SessionManager;

import java.util.Collection;
import java.util.List;

/**
 * The type Data base session manager.
 *
 * @author zhangsen
 * @data 2019 /4/4
 */
@LoadLevel(name = "db")
public class DataBaseSessionManager implements SessionManager, SessionLifecycleListener {

    @Override
    public void addGlobalSession(GlobalSession session) throws TransactionException {

    }

    @Override
    public GlobalSession findGlobalSession(Long transactionId) throws TransactionException {
        return null;
    }

    @Override
    public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException {

    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {

    }

    @Override
    public void addBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException {

    }

    @Override
    public void updateBranchSessionStatus(BranchSession session, BranchStatus status) throws TransactionException {

    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException {

    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return null;
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        return null;
    }

    @Override
    public void onBegin(GlobalSession globalSession) throws TransactionException {

    }

    @Override
    public void onStatusChange(GlobalSession globalSession, GlobalStatus status) throws TransactionException {

    }

    @Override
    public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession, BranchStatus status) throws TransactionException {

    }

    @Override
    public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {

    }

    @Override
    public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {

    }

    @Override
    public void onClose(GlobalSession globalSession) throws TransactionException {

    }

    @Override
    public void onEnd(GlobalSession globalSession) throws TransactionException {

    }
}
