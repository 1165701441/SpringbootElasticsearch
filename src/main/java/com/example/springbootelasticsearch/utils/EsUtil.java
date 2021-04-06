package com.example.springbootelasticsearch.utils;

import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.elasticsearch.client.RestHighLevelClient;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class EsUtil {


    @Autowired
    private  RestHighLevelClient transportClient;

    private static RestHighLevelClient client;

    /**
     * @PostContruct是spring框架的注解 spring容器初始化的时候执行该方法
     */
    @PostConstruct
    public void init() {
        client = this.transportClient;
    }
    /***
     * 创建索引
     * @param indexName
     * @return
     */
    public static boolean createIndex(String indexName){
        /**
         * 验证索引是否已经存在
         */

        if (isIndexExist(indexName)) {
            log.info("Index is exits!");
            return false;
        }else {
            CreateIndexRequest request = new CreateIndexRequest(indexName);
            try {
                CreateIndexResponse response = client.indices().create(request,RequestOptions.DEFAULT);
                return response.isAcknowledged();
            } catch (IOException e) {
                e.printStackTrace();
                log.error("createIndex error");
            }
            return false;
        }
    }

    /***
     * 删除索引
     * @param indexName
     * @return
     */
    public static boolean deleteIndex(String indexName){

        if (isIndexExist(indexName)){
            DeleteIndexRequest request = new DeleteIndexRequest(indexName);
            try {
                AcknowledgedResponse response = client.indices().delete(request,RequestOptions.DEFAULT);
                return  response.isAcknowledged();
            } catch (IOException e) {
                e.printStackTrace();
                log.error("deleteIndex error {}",e);
            }
        }else {
            log.info("Index is exits!");
        }
        return false;
    }

    /***
     * 验证索引是否存在
     * @param indexName
     * @return
     */
    public static boolean isIndexExist(String indexName){
        try {
          return client.indices().exists(new GetIndexRequest(indexName),RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("isIndexExist error");
        }
        return false;
    }

    /***
     * 获取所有索引名称
     * @return
     */
    public static  Set<String> getAllIndex(){
        GetAliasesRequest request = new GetAliasesRequest();
        try {
            GetAliasesResponse response = client.indices().getAlias(request,RequestOptions.DEFAULT);
            Map<String, Set<AliasMetaData>> aliases = response.getAliases();
            Set<String> indices = aliases.keySet();
            return indices;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("getAllIndex error");
        }
        return null;
    }

    /***
     *  添加数据随机生成id
     * @param indexName 索引名称
     * @param jsonObject json数据
     * @return
     */
    public static String addData(JSONObject jsonObject,String indexName){
        return addData(jsonObject,indexName,UUID.randomUUID().toString().replaceAll("-",""));
    }


    /***
     * 添加数据，正定id
     * @param indexName 索引名称
     * @param jsonObject json数据
     * @param id
     * @return
     */
    public static String addData(JSONObject jsonObject,String indexName,String id){
        IndexRequest request = new IndexRequest(indexName);
        request.id(id);
        request.source(jsonObject);
        request.timeout("1s");
        try {
            IndexResponse response = client.index(request,RequestOptions.DEFAULT);
            return response.getId();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("public static String addData error{}",e);
        }
        return null;
    }

    /***
     * 批量新增
     * @param list
     * @param indexName
     * @return
     */
    public static Boolean addDataByList(List<?> list, String indexName){
        BulkRequest request = new BulkRequest(indexName);
        for (int i = 0; i < list.size() ; i++) {
            request.add(new IndexRequest(indexName)
                    .id(UUID.randomUUID().toString().replaceAll("-",""))
                    .source(list.get(i),XContentType.JSON));
        }

        try {
            BulkResponse responses = client.bulk(request,RequestOptions.DEFAULT);
            return responses.hasFailures();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("public static Boolean addList{}",e);
        }
        return false;
    }

    /***
     * 根据id删除数据
     * @param indexName
     * @param id
     * @return
     */
    public static Boolean deleteDataById(String indexName,String id){
        DeleteRequest request = new DeleteRequest(indexName);
        request.id(id);
        try {
            DeleteResponse response = client.delete(request,RequestOptions.DEFAULT);
            return response.isFragment();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("  public static Boolean deleteDataById(String indexName,String id){}",e);
        }
        return false;
    }

    /***
     * 根据id修改数据
     * @param indexName
     * @param id
     */
    public static boolean updateDataById(JSONObject jsonObject,String indexName,String id){

        UpdateRequest request = new UpdateRequest(indexName,id);
        request.timeout("1s");
        request.doc(jsonObject,XContentType.JSON); //将对象封装丢进去 XContentType方法 将要传输的数据进行告知
        try {
            UpdateResponse response = client.update(request,RequestOptions.DEFAULT);
            return response.isFragment();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("  public static boolean updateDataById(JSONObject jsonObject,String indexName,String id){}",e);
        }
        return false;
    }

    /***
     * 根据id索引查询数据
     * @param indexName
     * @param id
     * @param fields 查询的列
     * @return
     */
    public static Map<String, Object> getDtaById(String indexName,String id,String fields){
        GetRequest request = new GetRequest(indexName);
        request.id(id);
        if (! StringUtils.isEmpty(fields)){
            String[] includes = fields.split(",");
            String[] excludes = Strings.EMPTY_ARRAY;
            FetchSourceContext fetchSourceContext = new FetchSourceContext(true, includes, excludes);
            request.fetchSourceContext(fetchSourceContext);
        }
        try {
           GetResponse response = client.get(request,RequestOptions.DEFAULT);
           return response.getSource();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("  public static boolean updateDataById(JSONObject jsonObject,String indexName,String id){}",e);
        }
        return null;
    }

    /***
     * 分页查询
     * @param index
     * @param startPage
     * @param pageSize
     * @param query
     * @param sortField
     * @param highlightField
     * @return
     */
    public static EsPage searchDataPage(String index, int startPage, int pageSize, QueryBuilder query, String sortField, String highlightField){
        SearchRequest request = new SearchRequest(index);

        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(query);
        builder.size(pageSize);
        builder.from(startPage);
        builder.explain(true);
        // 高亮
        if (!StringUtils.isEmpty(highlightField)){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field(highlightField);
            //highlightBuilder.preTags("<span style=\"color:red\">");
            //highlightBuilder.postTags("</span>");
            builder.highlighter(highlightBuilder);
        }
        // 排序
        if (!StringUtils.isEmpty(sortField)){
           builder.sort(new FieldSortBuilder(sortField).order(SortOrder.DESC));
        }

        request.source(builder);
        try {
            SearchResponse response = client.search(request,RequestOptions.DEFAULT);
            long total = response.getHits().getTotalHits().value;
            long length = response.getHits().getHits().length;

            List<Map<String,Object>> list = setSearchResponse(response,highlightField);
            EsPage page = new EsPage(startPage,pageSize,(int) total,list);
            return page;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("   public static EsPage searchDataPage(String index, int startPage, int pageSize, QueryBuilder query, String sortField, String highlightField){}",e);

        }
        return null;

    }

    /***
     * 使用分词查询
     * @param index
     * @param query
     * @param size
     * @param sortField
     * @param highlightField
     * @return
     */
    public static List<Map<String,Object>> searchListData(
            String index, QueryBuilder query, Integer size,
            String sortField, String highlightField) {
        SearchRequest request = new SearchRequest(index);
        SearchSourceBuilder builder = new SearchSourceBuilder();
        // 高亮
        if (!StringUtils.isEmpty(highlightField)){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field(highlightField);
            //highlightBuilder.preTags("<span style=\"color:red\">");
            //highlightBuilder.postTags("</span>");
            builder.highlighter(highlightBuilder);
        }
        // 排序
        if (!StringUtils.isEmpty(sortField)){
            builder.sort(new FieldSortBuilder(sortField).order(SortOrder.DESC));
        }
        builder.size(size);
        builder.query(query);
        request.source(builder);

        try {
            SearchResponse response = client.search(request,RequestOptions.DEFAULT);
            return setSearchResponse(response,highlightField);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 高亮结果集 特殊处理
     * @param response
     * @param highlightField
     * @return
     */
    private static List<Map<String,Object>>  setSearchResponse( SearchResponse response,String highlightField){
        //解析结果
        ArrayList<Map<String,Object>> list = new ArrayList<>();
        for (SearchHit hit: response.getHits().getHits()) {
            Map<String, HighlightField> high = hit.getHighlightFields();
            HighlightField title = high.get(highlightField);
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();//原来的结果
            //解析高亮字段,将原来的字段换为高亮字段
            if (title!=null){
                Text[] texts = title.fragments();
                String nTitle="";
                for (Text text : texts) {
                    nTitle+=text;
                }
                //替换
                sourceAsMap.put(highlightField,nTitle);
            }
            list.add(sourceAsMap);
        }
        return list;
    }


}
