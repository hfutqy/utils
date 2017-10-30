import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.lang3.StringUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ESClient {

    private static final Logger log = LoggerFactory.getLogger(ESClient.class);

    private TransportClient client;

    public ESClient() {
        this.init();
    }


    private void close() {
        if (this.client != null) {
            this.client.close();
        }
    }

    @Override
    public void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    private void init() {
        try {
            Settings settings = ImmutableSettings.settingsBuilder().loadFromClasspath("elasticsearch.yml")
                    .put("client.transport.sniff", true).build();
            this.client = new TransportClient(settings);

            int port = settings.getAsInt("client.transport.port", 9900);
            String[] ips = settings.getAsArray("client.transport.ip");

            for (String ip : ips) {
                log.info("the ip is:" + ip);
                client.addTransportAddress(new InetSocketTransportAddress(ip, port));
            }
            log.info("es连接成功:{},{}", client, JSONObject.toJSONString(client.listedNodes()));

        } catch (Exception e) {
            if (client != null) {
                client.close();
            }
            log.error("连接es失败!", e);
        }
    }

    public void createIndex(String index) throws Exception {
        client.admin().indices().prepareCreate(index).execute().actionGet();
    }

    /**
     * 为一种类型建立mapping
     *
     * @param index   索引名，相当于关系型数据库的库名
     * @param type    文档类型，相当于关系型数据库的表名
     * @param builder mapping内容, 格式 { "properties": { "fieldName1": { "type":
     *                "string", "analyzer": "ik" }, "fieldName2": { "type":
     *                "string", "index": "not_analyzed" } } }
     */
    public void mappingDoc(String index, String type, XContentBuilder builder)
            throws Exception {
        // XContentBuilder builder = XContentFactory.jsonBuilder()
        // .startObject()
        // .startObject("properties")
        // .startObject("province")
        // .field("type", "string")
        // //.field("store", "yes")
        // .field("analyzer","ik")
        // .field("index","analyzed")
        // //.field("indexAnalyzer", "ik")
        // //.field("searchAnalyzer", "ik")
        // .endObject()
        // .endObject()
        // .endObject();

        PutMappingRequest mapping = Requests.putMappingRequest(index)
                .type(type).source(builder);
        client.admin().indices().putMapping(mapping).actionGet();
    }

    /**
     * 为一份文档建立索引,采用自生成id
     *
     * @param index 索引名，相当于关系型数据库的库名
     * @param type  文档类型，相当于关系型数据库的表名
     * @param json  json格式的数据集
     * @return
     */
    public IndexResponse indexDoc(String index, String type, String json) throws Exception {
        IndexRequestBuilder builder = client.prepareIndex(index, type);
        IndexResponse response = builder.setSource(json)
                .execute()
                .actionGet();
        return response;
    }

    /**
     * 为一份文档建立索引,采用自生成id
     *
     * @param index 索引名，相当于关系型数据库的库名
     * @param type  文档类型，相当于关系型数据库的表名
     * @param kvMap 键值对形式的数据集
     * @return
     */
    public IndexResponse indexDoc(String index, String type, Map<String, Object> kvMap)
            throws Exception {
        IndexRequestBuilder builder = client.prepareIndex(index, type);
        IndexResponse response = builder.setSource(kvMap)
                .execute()
                .actionGet();
        return response;
    }

    /**
     * 为一份文档建立索引
     *
     * @param index 索引名，相当于关系型数据库的库名
     * @param type  文档类型，相当于关系型数据库的表名
     * @param id    文档id
     * @param json  json格式的数据集
     * @return
     */
    public IndexResponse indexDoc(String index, String type, String id, String json)
            throws Exception {

        IndexRequestBuilder builder = client.prepareIndex(index, type, id);
        IndexResponse response = builder.setSource(json)
                .execute()
                .actionGet();
        return response;
    }

    /**
     * 为一份文档建立索引
     *
     * @param index 索引名，相当于关系型数据库的库名
     * @param type  文档类型，相当于关系型数据库的表名
     * @param id    文档id
     * @param kvMap 键值对形式的数据集
     * @return
     */
    public IndexResponse indexDoc(String index, String type, String id, Map<String, Object> kvMap)
            throws Exception {
        IndexRequestBuilder builder = client.prepareIndex(index, type, id);
        IndexResponse response = builder.setSource(kvMap)
                .execute()
                .actionGet();
        return response;
    }


    /**
     * 为多份文档建立索引,采用自生成id
     *
     * @param index    索引名，相当于关系型数据库的库名
     * @param type     文档类型，相当于关系型数据库的表名
     * @param jsonList json格式的文档数据: List<json>
     * @return
     */
    public BulkResponse batchIndexDocsForJson(String index, String type, List<String> jsonList)
            throws Exception {
        if (jsonList.isEmpty()) {
            throw new Exception("批量创建索引时，传入的参数'jsonList'为空！");
        }

        List<IndexRequest> requestList = new ArrayList<IndexRequest>(jsonList.size());

        for (String json : jsonList) {
            IndexRequest request = client.prepareIndex(index, type)
                    .setSource(json)
                    .request();
            requestList.add(request);
        }

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (IndexRequest request : requestList) {
            bulkRequest.add(request);
        }

        BulkResponse response = bulkRequest
                .execute()
                .actionGet();

        return response;
    }


    /**
     * 为多份文档建立索引,采用自生成id
     *
     * @param index  索引名，相当于关系型数据库的库名
     * @param type   文档类型，相当于关系型数据库的表名
     * @param kvList 键值对形式的文档数据:List<Map<field, value>>
     * @return
     */
    public BulkResponse batchIndexDocsForMap(String index, String type, List<Map<String, Object>> kvList)
            throws Exception {
        if (kvList.isEmpty()) {
            throw new Exception("批量创建索引时，传入的参数'kvList'为空！");
        }

        List<String> jsonList = new ArrayList<String>(kvList.size());
        for (Map<String, Object> kvMap : kvList) {
            jsonList.add(JSONObject.toJSONString(kvMap));
        }

        BulkResponse response = this.batchIndexDocsForJson(index, type, jsonList);
        jsonList.clear();

        return response;
    }


    /**
     * 为多份文档建立索引
     *
     * @param index     索引名，相当于关系型数据库的库名
     * @param type      文档类型，相当于关系型数据库的表名
     * @param idJsonMap id及json格式的文档数据: Map<id,json>
     * @return
     */
    public BulkResponse batchIndexDocsForJson(String index, String type, Map<String, String> idJsonMap)
            throws Exception {
        if (idJsonMap.isEmpty()) {
            throw new Exception("批量创建索引时，传入的参数'idJsonMap'为空！");
        }

        List<IndexRequest> requestList = new ArrayList<IndexRequest>(idJsonMap.size());

        for (String id : idJsonMap.keySet()) {
            String json = idJsonMap.get(id);
            IndexRequest request = client.prepareIndex(index, type, id)
                    .setSource(json)
                    .request();
            requestList.add(request);
        }

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (IndexRequest request : requestList) {
            bulkRequest.add(request);
        }

        BulkResponse response = bulkRequest
                .execute()
                .actionGet();

        return response;
    }


    /**
     * 为多份文档建立索引
     *
     * @param index   索引名，相当于关系型数据库的库名
     * @param type    文档类型，相当于关系型数据库的表名
     * @param idKvMap id及键值对形式的文档数据:Map<id,Map<field, value>>
     * @return
     */
    public BulkResponse batchIndexDocsForMap(String index, String type, Map<String, Map<String, Object>> idKvMap)
            throws Exception {
        if (idKvMap.isEmpty()) {
            throw new Exception("批量创建索引时，传入的参数'idKvMap'为空！");
        }

        Map<String, String> idJsonMap = new HashMap<String, String>(idKvMap.size());
        for (String id : idKvMap.keySet()) {
            Map<String, Object> kvMap = idKvMap.get(id);
            idJsonMap.put(id, JSONObject.toJSONString(kvMap));
        }

        BulkResponse response = this.batchIndexDocsForJson(index, type, idJsonMap);
        idJsonMap.clear();

        return response;
    }

    /**
     * 更新一个doc, 若不存在则插入
     *
     * @param index
     * @param type
     * @param id
     * @param json
     * @param script
     * @throws java.util.concurrent.ExecutionException
     * @throws InterruptedException
     */
//    public UpdateResponse upsertDoc(String index, String type, String id, String json, String script) throws Exception {
//        IndexRequest indexRequest = new IndexRequest(index, type, id).source(json);
//        UpdateRequest updateRequest = new UpdateRequest(index, type, id);
//        //updateRequest.doc();
//        updateRequest.upsert(indexRequest);
//        updateRequest.script(script);
//
//        UpdateResponse response = client.update(updateRequest).get();
//
//        return response;
//    }
    
    public UpdateResponse upsertDoc(String index, String type, String id, String insertJson, String updateJson) throws Exception {
        IndexRequest indexRequest = new IndexRequest(index, type, id).source(insertJson);
        UpdateRequest updateRequest = new UpdateRequest(index, type, id);
        updateRequest.doc(updateJson);
        updateRequest.upsert(indexRequest);
        
        UpdateResponse response = client.update(updateRequest).get();

        return response;
    }

    /**
     * 根据条件 统计个数
     *
     * @param queryBuilder 查詢條件
     * @param index        索引库名 相當於 数据库名
     * @param type         索引类型 相當於 表名
     * @return
     */
    public long countQuery(String index, String type, QueryBuilder queryBuilder) {
        CountRequestBuilder crb = client.prepareCount(index).setTypes(type);
        if (queryBuilder != null) {
            crb.setQuery(queryBuilder);
        }

        CountResponse response = crb.execute().actionGet();
        return response.getCount();
    }

    public SearchResponse searchAgg(String index, String type, String searchType, QueryBuilder queryBuilder,
                                    AbstractAggregationBuilder aggBuilder) {
        SearchRequestBuilder builder = client.prepareSearch(index).setTypes(type);

        if (!StringUtils.isEmpty(searchType)) {
            builder.setSearchType(SearchType.valueOf(searchType));
        }

        if (queryBuilder != null) {
            builder = builder.setQuery(queryBuilder);
        }
        if (aggBuilder != null) {
            builder = builder.addAggregation(aggBuilder);
        }

        SearchResponse searchResponse = builder.execute().actionGet();

        return searchResponse;
    }

    /**
     * 删除一个文档
     *
     * @param index 索引名，相当于关系型数据库的库名
     * @param type  文档类型，相当于关系型数据库的表名
     * @param id    键值对形式的数据集
     * @return
     */
    public DeleteResponse deleteDoc(String index, String type, String id) throws InterruptedException {
        DeleteRequestBuilder builder = client.prepareDelete(index, type, id);
        DeleteResponse response = builder
                .execute()
                .actionGet();
        return response;
    }

    /**
     * 根据条件删除多个文档
     *
     * @param index        索引名，相当于关系型数据库的库名
     * @param type         文档类型，相当于关系型数据库的表名
     * @param queryBuilder 查询器
     * @return
     */
    public void deleteDocsByQuery(String index, String type, QueryBuilder queryBuilder) {
        client.prepareDeleteByQuery(index).setTypes(type).setQuery(queryBuilder)
                .execute()
                .actionGet();
    }


    /**
     * 指定id获取文档
     *
     * @param index 索引名，相当于关系型数据库的库名
     * @param type  文档类型，相当于关系型数据库的表名
     * @param id    文档id
     * @return
     */
    public Map<String, Object> getDoc(String index, String type, String id) {
        GetResponse response = client.prepareGet(index, type, id)
                .execute()
                .actionGet();

        Map<String, Object> retMap = response.getSourceAsMap();
        return retMap;
    }


    public List<Map<String, Object>> search(String index, String type, QueryBuilder queryBuilder, FilterBuilder filterBuilder) {
        SearchRequestBuilder builder = client.prepareSearch(index).setTypes(type);

        if (queryBuilder != null) {
            builder = builder.setQuery(queryBuilder);
        }
        if (filterBuilder != null) {
            builder = builder.setPostFilter(filterBuilder);
        }

        SearchResponse searchResponse = builder.execute().actionGet();

        SearchHits hits = searchResponse.getHits();
        log.info("Es Hits count: " + hits.getTotalHits());

        List<Map<String, Object>> kvList = new ArrayList<Map<String, Object>>();

        SearchHit[] hitArray = hits.getHits();
        if (hitArray.length > 0) {
            for (SearchHit hit : hitArray) {
                Map<String, Object> kvMap = hit.getSource();

                kvMap.put("version", hit.getVersion());
                kvMap.put("_id", hit.getId());
                kvList.add(kvMap);
            }
        }
        return kvList;
    }
}
```
