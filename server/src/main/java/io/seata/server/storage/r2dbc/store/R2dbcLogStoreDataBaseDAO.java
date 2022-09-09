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
package io.seata.server.storage.r2dbc.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.sql.DataSource;

import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.UUIDGenerator;
import io.seata.server.storage.db.store.LogStoreDataBaseDAO;
import io.seata.server.storage.r2dbc.entity.BranchTransaction;
import io.seata.server.storage.r2dbc.entity.GlobalTransaction;
import io.seata.server.storage.r2dbc.repository.GlobalTransactionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;

/**
 * The type Log store data base dao.
 *
 * @author jianbin.chen
 */
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
@Component
public class R2dbcLogStoreDataBaseDAO extends LogStoreDataBaseDAO {

    @Resource
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Resource
    private GlobalTransactionRepository globalTransactionRepository;

    BeanCopier globalEntityToDO = BeanCopier.create(GlobalTransaction.class, GlobalTransactionDO.class, false);
    BeanCopier globalDOToEntity = BeanCopier.create(GlobalTransactionDO.class, GlobalTransaction.class, false);

    BeanCopier branchEntityToDO = BeanCopier.create(BranchTransaction.class, BranchTransactionDO.class, false);
    
    BeanCopier branchDOToEntity = BeanCopier.create(BranchTransactionDO.class, BranchTransaction.class, false);

    public R2dbcLogStoreDataBaseDAO() {
        super();
    }

    /**
     * Instantiates a new Log store data base dao.
     *
     * @param logStoreDataSource the log store data source
     */
    public R2dbcLogStoreDataBaseDAO(DataSource logStoreDataSource) {
        super(logStoreDataSource);
        initTransactionNameSize();
    }

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(String xid) {
        GlobalTransaction globalTransaction = r2dbcEntityTemplate
            .selectOne(Query.query(Criteria.where(ServerTableColumnsName.GLOBAL_TABLE_XID).is(xid)), GlobalTransaction.class).block();
        if (globalTransaction == null) {
            return null;
        }
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalEntityToDO.copy(globalTransaction,globalTransactionDO,null);
        return globalTransactionDO;
    }

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(long transactionId) {
        GlobalTransaction globalTransaction = r2dbcEntityTemplate.selectOne(
            Query.query(Criteria.where(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID).is(transactionId)),
            GlobalTransaction.class).block();
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalEntityToDO.copy(globalTransaction, globalTransactionDO, null);
        return globalTransactionDO;
    }

    @Override
    public List<GlobalTransactionDO> queryGlobalTransactionDO(int[] statuses, int limit) {
        List<GlobalTransaction> list = r2dbcEntityTemplate
            .select(Query.query(Criteria.where(ServerTableColumnsName.GLOBAL_TABLE_STATUS)
                .in(Arrays.stream(statuses).parallel().boxed().toArray(Integer[]::new))).limit(limit), GlobalTransaction.class)
            .collectList().block();
        return list != null ? list.parallelStream().map(globalTransaction -> {
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            globalEntityToDO.copy(globalTransaction, globalTransactionDO, null);
            return globalTransactionDO;
        }).collect(Collectors.toList()) : null;
    }

    @Override
    public boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        GlobalTransaction globalTransaction = new GlobalTransaction();
        globalDOToEntity.copy(globalTransactionDO, globalTransaction, null);
        return r2dbcEntityTemplate.insert(GlobalTransaction.class).using(globalTransaction).block() != null;
    }

    @Override
    public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        Map<SqlIdentifier, Object> map = new HashMap<>();
        map.put(SqlIdentifier.quoted(ServerTableColumnsName.GLOBAL_TABLE_STATUS), globalTransactionDO.getStatus());
        Integer count = r2dbcEntityTemplate.update(GlobalTransaction.class).inTable(globalTable)
            .matching(
                Query.query(Criteria.where(ServerTableColumnsName.GLOBAL_TABLE_XID).is(globalTransactionDO.getXid())))
            .apply(Update.from(map)).block();
        return count != null && count > 0;
    }

    @Override
    public boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        Integer count = r2dbcEntityTemplate.delete(GlobalTransaction.class).from(globalTable)
            .matching(
                Query.query(Criteria.where(ServerTableColumnsName.GLOBAL_TABLE_XID).is(globalTransactionDO.getXid())))
            .all().block();
        return count != null && count > 0;
    }

    @Override
    public List<BranchTransactionDO> queryBranchTransactionDO(String xid) {
        return r2dbcEntityTemplate.select(Query.query(Criteria.where(ServerTableColumnsName.BRANCH_TABLE_XID).is(xid)),
            BranchTransaction.class).collectList().block().parallelStream().map(branchTransaction -> {
                BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
                branchEntityToDO.copy(branchTransaction, branchTransactionDO, null);
                return branchTransactionDO;
            }).collect(Collectors.toList());
    }

    @Override
    public List<BranchTransactionDO> queryBranchTransactionDO(List<String> xids) {
        return r2dbcEntityTemplate.select(Query.query(Criteria.where(ServerTableColumnsName.BRANCH_TABLE_XID).in(xids)),
                BranchTransaction.class).collectList().block().parallelStream().map(branchTransaction -> {
            BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
            branchEntityToDO.copy(branchTransaction, branchTransactionDO, null);
            return branchTransactionDO;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        BranchTransaction branchTransaction = new BranchTransaction();
        branchDOToEntity.copy(branchTransactionDO, branchTransaction, null);
        return r2dbcEntityTemplate.insert(BranchTransaction.class).using(branchTransaction).block() != null;
    }

    @Override
    public boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        BranchTransaction branchTransaction = new BranchTransaction();
        branchDOToEntity.copy(branchTransactionDO, branchTransaction, null);
        return r2dbcEntityTemplate.update(branchTransaction).block() != null;
    }

    @Override
    public boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        Integer count = r2dbcEntityTemplate.delete(
            Query.query(
                Criteria.where(ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID).is(branchTransactionDO.getBranchId())),
            BranchTransaction.class).block();
        return count != null && count > 0;
    }

    @Override
    public long getCurrentMaxSessionId(long high, long low) {
        return UUIDGenerator.generateUUID();
    }

}
