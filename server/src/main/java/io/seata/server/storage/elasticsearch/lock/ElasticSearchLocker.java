package io.seata.server.storage.elasticsearch.lock;

import io.seata.common.exception.ElasticSearchException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ElasticSearchUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockDO;
import io.seata.server.storage.elasticsearch.ClientConnectDAO;
import org.apache.http.HttpHost;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static io.seata.core.constants.ElasticSearchConstants.*;

/**
 * @author UmizzZ
 * @dat
 */
public class ElasticSearchLocker extends AbstractLocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchLocker.class);

    private static final HttpHost HTTP_HOST = new HttpHost("localhost",9200,"http");

    private ClientConnectDAO clientConnect = new ClientConnectDAO();;

    public ElasticSearchLocker(){
        //init connection
    }

    @Override
    public boolean acquireLock(List<RowLock> rowLock) {
        if(CollectionUtils.isEmpty(rowLock)){
            return true;
        }
        try{
            return acquireLockDO(convertToLockDO(rowLock));
        }
        catch(Exception e){
            throw e;
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> Locks) {
        if(CollectionUtils.isEmpty(Locks)){
            return true;
        }
        try{
            return unLock(convertToLockDO(Locks));
        }
        catch(Exception e){
            throw new ElasticSearchException(e);
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId){
        try{
            return unLock(xid, branchId);
        }
        catch(Exception e){
            throw new ElasticSearchException(e);
        }
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds){
        if (CollectionUtils.isEmpty(branchIds)) {
            return true;
        }
        try{
            return unLock(xid, branchIds);
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
    }
    @Override
    public boolean isLockable(List<RowLock> rowLock) {
        if(CollectionUtils.isEmpty(rowLock)){
            return true;
        }
        try{
            return isLockableDO(convertToLockDO(rowLock));
        }
        catch(Exception e) {
            throw new ElasticSearchException(e);
        }
    }

    private boolean acquireLockDO(List<LockDO> lockDOs){
        //实际的DO层
        RestHighLevelClient client = clientConnect.ClientConnect();
        if (lockDOs.size() > 1) {
            lockDOs = lockDOs.stream().filter(LambdaUtils.distinctByKey(LockDO::getRowKey)).collect(Collectors.toList());
        }
        try{
            //query
            boolean canlock = true;
            Set<String> esExistLockRowKeys = new HashSet<>();
            String currentXid = lockDOs.get(0).getXid();
            List<String> rowKeys = new ArrayList<>();
            for(int i = 0; i<lockDOs.size(); i++){
                rowKeys.add(lockDOs.get(i).getRowKey());
            }
            SearchRequest request = new SearchRequest();
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termsQuery(ES_INDEX_LOCK_ROWKEY, rowKeys));
            request.indices(DEFAULT_LOCK_INDEX);
            request.source(sourceBuilder);
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            TotalHits totalHits = searchHits.getTotalHits();
            long numHits = totalHits.value;
            if(numHits > 0) {
                SearchHit[] hits = searchHits.getHits();
                for (SearchHit searchHit : hits) {
                    String lockIndex = searchHit.getIndex();
                    String rowKey = searchHit.getId();
                    Map<String, Object> source = searchHit.getSourceAsMap();
                    Object EsXid = findSourceValue(source, ES_INDEX_LOCK_XID);
                    if (EsXid != null && currentXid != null && !StringUtils.equals(currentXid, String.valueOf(EsXid))) {
                        //被其他锁持有
                        if (LOGGER.isInfoEnabled()) {
                            Object EsPk = findSourceValue(source, ES_INDEX_LOCK_PK);
                            Object EsBranchId = findSourceValue(source, ES_INDEX_LOCK_BRANCH_ID);
                            LOGGER.info("Global lock on [{}:{}] is holding by xid {} branchId {}", lockIndex, EsPk, EsXid, EsBranchId);
                        }
                        canlock = false;
                        break;
                    }
                    esExistLockRowKeys.add(String.valueOf(findSourceValue(source, ES_INDEX_LOCK_ROWKEY)));
                }
            }
            //query finish
            if (!canlock){
                return false;
            }
            List<LockDO> unrepeatedLockDODs;
            if(CollectionUtils.isNotEmpty(esExistLockRowKeys)){
                unrepeatedLockDODs = lockDOs.stream().filter(lockDO -> !esExistLockRowKeys.contains(lockDO.getRowKey())).collect(Collectors.toList());
            } else {
                unrepeatedLockDODs = lockDOs;
            }
            if (CollectionUtils.isEmpty(unrepeatedLockDODs)){
                return true;
            }

            if(unrepeatedLockDODs.size() == 1){
                LockDO lockDO = unrepeatedLockDODs.get(0);
                if(!insertLockDO(client, lockDO)){
                    if(LOGGER.isInfoEnabled()){
                        LOGGER.info("Global lock acquire failed, xid {} branchId {} pks {}", lockDO.getXid(), lockDO.getBranchId(), lockDO.getPk());
                    }
                    return false;
                }
            } else {
                if (!insertLockDO(client, unrepeatedLockDODs)){
                    if (LOGGER.isInfoEnabled()){
                        LOGGER.info("Global lock batch acquire failed, xid{} branchId{} pks{}", unrepeatedLockDODs.get(0).getXid(),
                                unrepeatedLockDODs.get(0).getBranchId(), unrepeatedLockDODs.stream().map(lockDO -> lockDO.getPk()).collect(Collectors.toList()));
                    }
                    return false;
                }
            }
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            throw new ElasticSearchException(e);
        }
    }

    private boolean insertLockDO(RestHighLevelClient client, LockDO lockDO){
        IndexRequest request = new IndexRequest();
        request.index(DEFAULT_LOCK_INDEX);
        request.id(lockDO.getRowKey());
        Map<String, Object> jsonMap;
        jsonMap = ElasticSearchUtils.toESjsonMap(lockDO, ES_INDEX_LOCK_ROWKEY);
        request.source(jsonMap);
        try{
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private boolean insertLockDO(RestHighLevelClient client, List<LockDO> lockDOS){
        //批量插入操作
        BulkRequest request = new BulkRequest();
        for(LockDO lockDO : lockDOS){
            Map<String, Object> jsonMap = ElasticSearchUtils.toESjsonMap(lockDO, ES_INDEX_LOCK_ROWKEY);
            request.add(new IndexRequest(DEFAULT_LOCK_INDEX).id(lockDO.getRowKey()).source(jsonMap));
        }
        try{
            BulkResponse responses = client.bulk(request, RequestOptions.DEFAULT);
        }
        catch(Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private boolean unLock(List<LockDO> lockDOS){
        LOGGER.info("Lock is releasing List<LockDO> lockDOS");
        BulkRequest request = new BulkRequest();
        RestHighLevelClient client = clientConnect.ClientConnect();
        for(LockDO lockDO : lockDOS){
            request.add(new DeleteRequest(DEFAULT_LOCK_INDEX).id(lockDO.getRowKey()));
        }
        try{
            BulkResponse responses = client.bulk(request, RequestOptions.DEFAULT);
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private boolean unLock(String xid, Long branchId){
        LOGGER.info("Lock is releasing List<LockDO> lockDOS");
        DeleteByQueryRequest request = new DeleteByQueryRequest();
        RestHighLevelClient client = clientConnect.ClientConnect();
        request.indices(DEFAULT_LOCK_INDEX);
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(ES_INDEX_LOCK_XID, xid))
                .must(QueryBuilders.termQuery(ES_INDEX_LOCK_BRANCH_ID, branchId));
        request.setQuery(queryBuilder);
        try{
            BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private boolean unLock(String xid, List<Long> branchIds){
        LOGGER.info("Lock is releasing List<LockDO> lockDOS");
        DeleteByQueryRequest request = new DeleteByQueryRequest();
        RestHighLevelClient client = clientConnect.ClientConnect();
        request.indices(DEFAULT_LOCK_INDEX);
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(ES_INDEX_LOCK_XID, xid))
                .must(QueryBuilders.termQuery(ES_INDEX_LOCK_BRANCH_ID, branchIds));
        request.setQuery(queryBuilder);
        try{
            BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);
        }
        catch(Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }
    private boolean unLock(LockDO lockDO){
        return unLock(Collections.singletonList(lockDO));
    }

    private boolean isLockableDO(List<LockDO> lockDOs){
        String currentXid = lockDOs.get(0).getXid();
        MultiGetRequest request = new MultiGetRequest();
        RestHighLevelClient client = clientConnect.ClientConnect();
        for(LockDO lockDO : lockDOs){
            String rowKey = lockDO.getRowKey();
            request.add(new MultiGetRequest.Item(DEFAULT_LOCK_INDEX,rowKey));
        }
        try{
            MultiGetResponse response = client.mget(request, RequestOptions.DEFAULT);
            for(MultiGetItemResponse Item : response.getResponses()){
                if(!Item.isFailed()){
                    //子请求成功查询
                    String index = Item.getIndex();
                    String resultRowKey = Item.getId();
                    GetResponse Get = Item.getResponse();
                    Map<String, Object> source = Get.getSourceAsMap();
                    if(StringUtils.equals(index, DEFAULT_LOCK_INDEX)){
                        Object resultXid = findSourceValue(source, ES_INDEX_LOCK_XID);
                        if(!StringUtils.equals(currentXid, String.valueOf(resultXid))){
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
    }

    private Object findSourceValue(Map<String, Object> source, String keyName){
        Object result = null;
        for(String key: source.keySet()){
            if(StringUtils.equals(key, keyName)){
                result = source.get(key);
                break;
            }
        }
        return result;
    }
}
