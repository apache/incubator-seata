package io.seata.server.storage.elasticsearch.store;

import io.seata.common.exception.ElasticSearchException;
import io.seata.common.exception.StoreException;
import io.seata.common.util.BeanUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ElasticSearchUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.elasticsearch.ClientConnectDAO;
import io.seata.server.storage.redis.store.RedisTransactionStoreManager;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import jdk.nashorn.internal.ir.JumpToInlinedFinally;
import org.apache.http.HttpHost;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongBiFunction;
import java.util.stream.Collectors;

import static io.seata.core.constants.ElasticSearchConstants.*;
//常量这一块应该再增加一点... 增加各个表名的...统一一下 需要修改的代码位置 "xid" "transactionId" toEsjonMap可能需要？

/**
 * @author UmizzZ
 * @date
 */
public class ElasticSearchTransactionStoreManager extends AbstractTransactionStoreManager implements  TransactionStoreManager{

    private static volatile ElasticSearchTransactionStoreManager instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchTransactionStoreManager.class);


    private ClientConnectDAO clientConnect = new ClientConnectDAO();

    public static ElasticSearchTransactionStoreManager getInstance(){
        if(instance == null){
            synchronized (ElasticSearchTransactionStoreManager.class){
                if (instance == null){
                    instance = new ElasticSearchTransactionStoreManager();
                }
            }
        }
        return instance;
    }


    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session){
        if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
            return insertGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
            return updateGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
            return deleteGlobalTransactionDO(SessionConverter.convertGlobalTransactionDO(session));
        } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
            return insertBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
            return updateBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
            return deleteBranchTransactionDO(SessionConverter.convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }


    @Override
    public GlobalSession readSession(String xid) {
        return this.readSession(xid, true);
    }

    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions){
        GlobalTransactionDO globalTransactionDO = queryGlobalTransactionDO(xid);
        if (globalTransactionDO == null){
            return null;
        }
        //branch Transaction
        List<BranchTransactionDO> branchTransactionDOS = null;
        //reduce rpc with ?
        if(withBranchSessions){
            branchTransactionDOS = queryBranchTransactionDO(globalTransactionDO.getXid());
        }
        return getGlobalSession(globalTransactionDO, branchTransactionDOS);
    }

    public GlobalSession readSession(Long transactionId){
        //global transaction
        GlobalTransactionDO globalTransactionDO = queryGlobalTransactionDO(transactionId);
        if (globalTransactionDO == null){
            return null;
        }
        //branch transaction
        List<BranchTransactionDO> branchTransactionDOS = queryBranchTransactionDO(globalTransactionDO.getXid());
        return getGlobalSession(globalTransactionDO, branchTransactionDOS);
    }

    public List<GlobalSession> readSession(GlobalStatus[] statuses){
        int[] states = new int[statuses.length];
        for (int i = 0; i <states.length; i++){
            states[i] = statuses[i].getCode();
        }
        //global transaction
        List<GlobalTransactionDO> globalTransactionDOS = queryGlobalTransaction(states);

        if (CollectionUtils.isEmpty(globalTransactionDOS)){
            return null;
        }
        List<String> xids = globalTransactionDOS.stream().map(GlobalTransactionDO::getXid).collect(Collectors.toList());
        List<BranchTransactionDO> branchTransactionDOS = queryBranchTransactionDO(xids);

        Map<String, List<BranchTransactionDO>> branchTransactionDOsMap = branchTransactionDOS.stream()
                .collect(Collectors.groupingBy(BranchTransactionDO::getXid, LinkedHashMap::new, Collectors.toList()));
        return globalTransactionDOS.stream().map(globalTransactionDO ->
                getGlobalSession(globalTransactionDO, branchTransactionDOsMap.get(globalTransactionDO.getXid()))).collect(Collectors.toList());
    }

    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition){
        if (StringUtils.isNotBlank(sessionCondition.getXid())) {
            GlobalSession globalSession = readSession(sessionCondition.getXid());
            if (globalSession != null) {
                List<GlobalSession> globalSessions = new ArrayList<>();
                globalSessions.add(globalSession);
                return globalSessions;
            }
        } else if (sessionCondition.getTransactionId() != null) {
            GlobalSession globalSession = readSession(sessionCondition.getTransactionId());
            if (globalSession != null) {
                List<GlobalSession> globalSessions = new ArrayList<>();
                globalSessions.add(globalSession);
                return globalSessions;
            }
        } else if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
            return readSession(sessionCondition.getStatuses());
        }
        return null;
    }

    private GlobalTransactionDO queryGlobalTransactionDO(String xid) {
        GlobalTransactionDO res;
        RestHighLevelClient client = clientConnect.ClientConnect();
        GetRequest request = new GetRequest();
        request.index(DEFAULT_GLOBAL_INDEX);
        request.id(xid);
        try {
            GetResponse response = client.get(request, RequestOptions.DEFAULT);
            String index = response.getIndex();
            String id = response.getId();
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            if (response.isExists()) {
                long version = response.getVersion();
                Map<String, Object> result = response.getSourceAsMap();
                globalTransactionDO = convertToGlobalTransactionDO(id, result);
            }
            res = globalTransactionDO;
        } catch (Exception e) {
            throw new ElasticSearchException(e);
        } finally {
            clientConnect.ClientShutdown(client);
        }
        return res;
    }

    private GlobalTransactionDO queryGlobalTransactionDO(long transactionId){
        GlobalTransactionDO res;
        RestHighLevelClient client = clientConnect.ClientConnect();
        SearchRequest request = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery(ES_INDEX_GLOBAL_TRANSACTION_ID, transactionId));
        request.indices(DEFAULT_GLOBAL_INDEX);
        request.source(sourceBuilder);
        try{
            Map<String, Object> source;
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            TotalHits totalHits = hits.getTotalHits();
            long numHits = totalHits.value;
            if(numHits == 0){
                return null;
            }
            if(numHits == 1) {
                SearchHit[] searchHits = hits.getHits();
                SearchHit searchHit = searchHits[0];
                String index = searchHit.getIndex();
                String xid = searchHit.getId();
                source = searchHit.getSourceAsMap();
                if(index == DEFAULT_GLOBAL_INDEX) {
                    res = convertToGlobalTransactionDO(xid, source);
                    return res;
                }
            }
            else{
                LOGGER.error("The number of the GlobalTransaction is greater than one ");
            }
        }
        catch(Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return null;
    }

    private List<GlobalTransactionDO> queryGlobalTransaction(int[] statues){
        List<GlobalTransactionDO> res = null;
        GlobalTransactionDO globalTransactionDO;
        RestHighLevelClient client = clientConnect.ClientConnect();
        SearchRequest request = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termsQuery(ES_INDEX_GLOBAL_STATUS,statues));
        request.indices(DEFAULT_GLOBAL_INDEX);
        request.source(sourceBuilder);
        try{
            Map<String, Object> source;
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            TotalHits totalHits = hits.getTotalHits();
            long numHits = totalHits.value;
            if(numHits == 0){
                return null;
            }
            SearchHit[] searchHits = hits.getHits();
            for(SearchHit searchHit: searchHits){
                String index =searchHit.getIndex();
                String xid = searchHit.getId();
                source = searchHit.getSourceAsMap();
                if(index == DEFAULT_BRANCH_INDEX){
                    globalTransactionDO = convertToGlobalTransactionDO(xid, source);
                    res.add(globalTransactionDO);
                }
            }
            return res;
        }
        catch(Exception e){
            throw new ElasticSearchException(e);
        }
        finally{
            clientConnect.ClientShutdown(client);
        }
    }

    private List<BranchTransactionDO> queryBranchTransactionDO(String xid){
        List<BranchTransactionDO> res = null;
        BranchTransactionDO branchTransactionDO;
        RestHighLevelClient client = clientConnect.ClientConnect();
        SearchRequest request = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery(ES_INDEX_BRANCH_XID,xid));
        request.indices(DEFAULT_BRANCH_INDEX);
        request.source(sourceBuilder);
        try{
            Map<String, Object> source;
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            TotalHits totalHits = hits.getTotalHits();
            long numHits =totalHits.value;
            if(numHits == 0){
                return null;
            }
            SearchHit[] searchHits = hits.getHits();
            for(SearchHit searchHit: searchHits){
                String index = searchHit.getIndex();
                String branchId = searchHit.getId();
                source = searchHit.getSourceAsMap();
                if(index == DEFAULT_BRANCH_INDEX) {
                    branchTransactionDO = convertToBranchTransactionDO(Long.valueOf(branchId), source);
                    res.add(branchTransactionDO);
                }
            }
            return res;
        }
        catch(Exception e){
            throw new ElasticSearchException(e);
        }
        finally{
            clientConnect.ClientShutdown(client);
        }
    }

    private List<BranchTransactionDO> queryBranchTransactionDO(List<String> xids){
        List<BranchTransactionDO> res = null;
        BranchTransactionDO branchTransactionDO;
        RestHighLevelClient client = clientConnect.ClientConnect();
        SearchRequest request = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termsQuery(ES_INDEX_BRANCH_XID, xids));
        request.indices(DEFAULT_BRANCH_INDEX);
        request.source(sourceBuilder);
        try{
            Map<String, Object> source;
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            TotalHits totalHits = hits.getTotalHits();
            long numHits = totalHits.value;
            if(numHits == 0){
                return null;
            }
            SearchHit[] searchHits = hits.getHits();
            for(SearchHit searchHit: searchHits){
                String index =searchHit.getIndex();
                String branchId = searchHit.getId();
                source = searchHit.getSourceAsMap();
                if(index == DEFAULT_BRANCH_INDEX){
                    branchTransactionDO = convertToBranchTransactionDO(Long.valueOf(branchId), source);
                    res.add(branchTransactionDO);
                }
            }
            return res;
        }
        catch(Exception e){
            throw new ElasticSearchException(e);
        }
        finally{
            clientConnect.ClientShutdown(client);
        }
    }

    private boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO){
        RestHighLevelClient client = clientConnect.ClientConnect();
        IndexRequest request = new IndexRequest();
        request.index(DEFAULT_GLOBAL_INDEX);
        request.id(globalTransactionDO.getXid());
        Map<String, Object> jsonMap;
        jsonMap = ElasticSearchUtils.toESjsonMap(globalTransactionDO,ES_INDEX_GLOBAL_XID);
        request.source(jsonMap);
        try{
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        }
        catch (Exception e){
            throw new ElasticsearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO){
        LOGGER.info("deleting GlobalTransaction");
        RestHighLevelClient client = clientConnect.ClientConnect();
        DeleteRequest request = new DeleteRequest();
        request.index(DEFAULT_GLOBAL_INDEX);
        request.id(globalTransactionDO.getXid());
        try{
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        LOGGER.info("updating GlobalTransaction");
        RestHighLevelClient client = clientConnect.ClientConnect();
        UpdateRequest request = new UpdateRequest();
        request.index(DEFAULT_GLOBAL_INDEX);
        request.id(globalTransactionDO.getXid());
        Map<String, Object> jsonMap;
        jsonMap = ElasticSearchUtils.toESjsonMap(globalTransactionDO,ES_INDEX_GLOBAL_XID);
        request.doc(jsonMap);
        try{
            UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO){
        RestHighLevelClient client = clientConnect.ClientConnect();
        IndexRequest request = new IndexRequest();
        request.index(DEFAULT_BRANCH_INDEX);
        request.id(String.valueOf(branchTransactionDO.getBranchId()));
        Map<String, Object> jsonMap;
        jsonMap = ElasticSearchUtils.toESjsonMap(branchTransactionDO,ES_INDEX_BRANCH_BRANCH_ID);
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

    private boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        LOGGER.info("deleting BranchTransaction");
        RestHighLevelClient client = clientConnect.ClientConnect();
        DeleteRequest request = new DeleteRequest();
        request.index(DEFAULT_BRANCH_INDEX);
        request.id(String.valueOf(branchTransactionDO.getTransactionId()));
        try{
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
        finally{
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        RestHighLevelClient client = clientConnect.ClientConnect();
        UpdateRequest request = new UpdateRequest();
        request.index(DEFAULT_BRANCH_INDEX);
        request.id(String.valueOf(branchTransactionDO.getTransactionId()));
        try{
            UpdateResponse response =client.update(request, RequestOptions.DEFAULT);
        }
        catch (Exception e){
            throw new ElasticSearchException(e);
        }
        finally {
            clientConnect.ClientShutdown(client);
        }
        return true;
    }

    private GlobalTransactionDO convertToGlobalTransactionDO(String xid, Map<String, Object> source){
//        Map to Object  Note: 大小写？如何处理
        // 将Object to String!
        Map<String, String> map;
        map = ElasticSearchUtils.toStringMap(source);
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO = (GlobalTransactionDO) BeanUtils.mapToObject(map, GlobalTransactionDO.class);
        globalTransactionDO.setXid(xid);
        //云清兄有封装好的MapToObject!属实牛皮 跪了 QAQ 只要<String, String> to object
        return globalTransactionDO;
    }

    private BranchTransactionDO convertToBranchTransactionDO(long branchId, Map<String, Object> source){
        //Map <String, Object> to object;
        //还没写 晚点写
        Map<String, String> map;
        map = ElasticSearchUtils.toStringMap(source);
        BranchTransactionDO branchTransactionDO;
        branchTransactionDO = (BranchTransactionDO) BeanUtils.mapToObject(map, BranchTransactionDO.class);
        branchTransactionDO.setBranchId(branchId);
        return branchTransactionDO;
    }

    private GlobalSession getGlobalSession(GlobalTransactionDO globalTransactionDO,
                                           List<BranchTransactionDO> branchTransactionDOs) {
        GlobalSession globalSession = SessionConverter.convertGlobalSession(globalTransactionDO);
        //branch transactions
        if (CollectionUtils.isNotEmpty(branchTransactionDOs)) {
            for (BranchTransactionDO branchTransactionDO : branchTransactionDOs) {
                globalSession.add(SessionConverter.convertBranchSession(branchTransactionDO));
            }
        }
        return globalSession;
    }
}
