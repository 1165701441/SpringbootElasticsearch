package com.example.springbootelasticsearch.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.springbootelasticsearch.utils.DateUtil;
import com.example.springbootelasticsearch.utils.EsPage;
import com.example.springbootelasticsearch.utils.EsUtil;
import com.example.springbootelasticsearch.vo.Employee;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/es")
public class EsController {

    /**
     * 测试索引
     */
    private String indexName = "megacorp";



    /**
     * 创建索引
     * http://127.0.0.1:8080/es/createIndex
     * @return
     */
    @GetMapping("/createIndex")
    public String createIndex() {
        if (!EsUtil.isIndexExist(indexName)) {
            EsUtil.createIndex(indexName);
        } else {
            return "索引已经存在";
        }
        return "索引创建成功";
    }

    /***
     * 新增不带id
     * @param employee
     * @return
     */
    @PostMapping("/insertJson")
    public String insertJson(@RequestBody Employee employee){
        employee.setId(DateUtil.formatDate(new Date()));
        JSONObject json = (JSONObject) JSONObject.toJSON(employee);
        String id = EsUtil.addData(json,indexName);
        return id;
    }

    /***
     * 新增带id
     * @param employee
     * @return
     */
    @PostMapping("/insertJsonById")
    public String insertJsonById(@RequestBody Employee employee){
        if (employee.getId() == null){
            return "id为空";
        }else {
            JSONObject json = (JSONObject) JSONObject.toJSON(employee);

            String id = EsUtil.addData(json, indexName,employee.getId());
            return id;
        }
    }

    /***
     * 修改
     * @param employee
     * @return
     */
    @PostMapping("/updateJson")
    public String updateJson(@RequestBody Employee employee){
        if (employee.getId() == null){
            return "id为空";
        }else {
            JSONObject json = (JSONObject) JSONObject.toJSON(employee);
            EsUtil.updateDataById(json,indexName,employee.getId());

            return "id"+ employee.getId();
        }
    }

    /**
     * 根据编号查询
     * @param id
     * @return
     */
    @GetMapping("/getDataById/{id}")
    public String getDataById(@PathVariable("id") String id){
        if (!StringUtils.isEmpty(id)){
            Map<String, Object> map = EsUtil.getDtaById(indexName,id,"id,firstName");
            return JSONObject.toJSONString(map);
        }
        return "id为空";
    }

    @DeleteMapping("/deleteById/{id}")
    public String deleteById(@PathVariable("id")String id){
        if (!StringUtils.isEmpty(id)){
            EsUtil.deleteDataById(indexName,id);
            return "删除成功";
        }
        return "id为空";
    }

    /**
     * 查询数据
     * 模糊查询
     *
     * @return
     */
    @GetMapping("/queryMatchData/{firstName}/{lastName}")
    public String queryMatchData(@PathVariable("lastName") String lastName,@PathVariable("firstName") String firstName) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolean matchPhrase = false;
        if (matchPhrase == Boolean.TRUE) {
            //不进行分词搜索
            boolQuery.must(QueryBuilders.matchPhraseQuery("firstName", firstName));
        } else {
            boolQuery.must(QueryBuilders.matchQuery("lastName", lastName));
        }




        // boolQueryBuilder.must(QueryBuilders.matchQuery("last_name", lastName));
        // 模糊查询
      //  boolQueryBuilder.filter(QueryBuilders.wildcardQuery("first_name", firstName));
        // 范围查询 from:相当于闭区间; gt:相当于开区间(>) gte:相当于闭区间 (>=) lt:开区间(<) lte:闭区间 (<=)
        boolQuery.filter(QueryBuilders.rangeQuery("age").from(0).to(8899));

        List<Map<String, Object>> list = EsUtil.
                searchListData(indexName, boolQuery, 10, "firstName", null);
        return JSONObject.toJSONString(list);
    }

    /**
     * 通配符查询数据
     * 通配符查询 ?用来匹配1个任意字符，*用来匹配零个或者多个字符
     *
     * @return
     */
    @GetMapping("/queryWildcardData/{keyword}")
    public String queryWildcardData(@PathVariable("keyword")String keyword) {
        QueryBuilder queryBuilder = QueryBuilders.wildcardQuery("firstName.keyword", keyword);
        List<Map<String, Object>> list = EsUtil.searchListData(indexName, queryBuilder, 10, null, null);
        return JSONObject.toJSONString(list);
    }

    /**
     * 正则查询
     *
     *
     * @return
     */
    @GetMapping("/queryRegexpData")
    public String queryRegexpData() {
        QueryBuilder queryBuilder = QueryBuilders.regexpQuery("first_name.keyword", "m--[0-9]{1,11}");
        List<Map<String, Object>> list = EsUtil.searchListData(indexName, queryBuilder, 10, null, null);
        return JSONObject.toJSONString(list);
    }

    /**
     * 查询数字范围数据
     *
     * @return
     */
    @GetMapping("/queryIntRangeData")
    public String queryIntRangeData() {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.rangeQuery("age").from(0).to(8899));

        List<Map<String, Object>> list = EsUtil.searchListData(indexName, boolQuery, 10, null, null);
        return JSONObject.toJSONString(list);
    }

    /**
     * 查询日期范围数据
     *
     * @return
     */
    @GetMapping("/queryDateRangeData")
    public String queryDateRangeData() {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.rangeQuery("age").from(0).to(8899));

        List<Map<String, Object>> list = EsUtil.searchListData(indexName, boolQuery, 10, null, null);
        return JSONObject.toJSONString(list);
    }

    /**
     * 查询分页
     *
     * @param startPage 第几条记录开始
     *                  从0开始
     *                  第1页 ：http://127.0.0.1:8080/es/queryPage?startPage=0&pageSize=2
     *                  第2页 ：http://127.0.0.1:8080/es/queryPage?startPage=2&pageSize=2
     * @param pageSize  每页大小
     * @return
     */
    @GetMapping("/queryPage/{startPage}/{pageSize}")
    public String queryPage(@PathVariable("startPage") String startPage,@PathVariable("pageSize") String pageSize) {
        if (!StringUtils.isEmpty(startPage) && !StringUtils.isEmpty(pageSize)) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.filter(QueryBuilders.rangeQuery("age").from(0).to(8899));

            EsPage list = EsUtil.searchDataPage(indexName, Integer.parseInt(startPage), Integer.parseInt(pageSize), boolQuery, null, null);
            return JSONObject.toJSONString(list);
        } else {
            return "startPage或者pageSize缺失";
        }
    }


}
