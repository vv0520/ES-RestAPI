import com.alibaba.fastjson.JSON;
import com.itgeima.es.entity.Goods;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
//静态导入，可导入任意类的任意成员当作本类成员
import static com.itgeima.es.entity.EsConstents.SOURCE_TEMPLATE;

public class ElasticDemo {

    private RestHighLevelClient client;

    /**
     * 建立连接
     */
    @Before
    public void init() throws IOException {
        client = new RestHighLevelClient(
                RestClient.builder(
                        HttpHost.create("http://ly-es:9200")
                )
        );
    }

    //创建索引库
    @Test
    public void testCreateIndex() throws IOException {
        //1.准备request对象
        CreateIndexRequest request = new CreateIndexRequest("goods");
        //2.给request对象准备请求参数
        request.source(SOURCE_TEMPLATE,XContentType.JSON);
        //3.发送request请求，得到response
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        System.out.println("response = " + response.isAcknowledged());

    }
    //导入文档数据
    @Test
    public void testBulkDocument() throws IOException {
        // 1.准备文档数据
        List<Goods> list = new ArrayList<>();
        list.add(new Goods(1L, Arrays.asList("红米9", "手机"), "红米9手机 数码", 1499L));
        list.add(new Goods(2L, Arrays.asList("三星", "Galaxy", "手机"), "三星 Galaxy A90 手机 数码 疾速5G 骁龙855", 3099L));
        list.add(new Goods(3L, Arrays.asList("Sony", "WH-1000XM3", "数码"), "Sony WH-1000XM3 降噪耳机 数码", 2299L));
        list.add(new Goods(4L, Arrays.asList("松下", "剃须刀"), "松下电动剃须刀高转速磁悬浮马达", 599L));

        // 2.创建BulkRequest对象
        BulkRequest bulkRequest = new BulkRequest();
        // 3.创建多个IndexRequest对象，并添加到BulkRequest中
        for (Goods goods : list) {
            bulkRequest.add(new IndexRequest("goods")
                    .id(goods.getId().toString())
                    .source(JSON.toJSONString(goods), XContentType.JSON)
            );
        }
        // 4.发起请求
        BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        System.out.println("status: " + bulkResponse.status());
    }

    //基本查询
    @Test
    public void testBasicSearchWithSortAndPage() throws IOException, InvocationTargetException, IllegalAccessException {
        // 1.创建SearchSourceBuilder对象
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 1.0.source过滤
        sourceBuilder.fetchSource(new String[0], new String[]{"name"});
        // 1.1.添加查询条件QueryBuilders，这里选择布尔查询，查询标题包含“数码”，并且价格小于3000
        // 1.1.1.定义布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1.1.2.添加match查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", "数码"));
        // 1.1.3.添加价格过滤
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(3000));
        sourceBuilder.query(boolQueryBuilder);//组合查询条件
        // 1.2.添加排序、分页等其它条件
        sourceBuilder.sort("price", SortOrder.ASC);
        // 1.3.添加分页条件
        int page = 1, size = 5;
        int from = (page - 1) * size;
        sourceBuilder.from(from);
        sourceBuilder.size(size);
        // 1.4.高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title"));
        // 2.创建SearchRequest对象，并制定索引库名称
        SearchRequest request = new SearchRequest("goods");
        // 2.1.添加SearchSourceBuilder对象到SearchRequest对象中
        request.source(sourceBuilder);
        // 3.发起请求，得到结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析结果
        SearchHits searchHits = response.getHits();
        //  4.1.获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("total = " + total);
        //  4.2.获取SearchHits数组，并遍历
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //  - 获取其中的`_source`，是JSON数据
            String json = hit.getSourceAsString();
            //  - 把`_source`反序列化为User对象
            Goods goods = JSON.parseObject(json, Goods.class);
            // 获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            // 遍历高亮字段
            for (HighlightField field : highlightFields.values()) {
                // 获取字段名
                String fieldName = field.getName();
                // 获取字段值
                String fieldValue = StringUtils.join(field.getFragments());
                // 注入对象中
                BeanUtils.setProperty(goods, fieldName, fieldValue);
            }
            System.out.println("goods = " + goods);
        }
    }

    //演示自动补全查询
    @Test
    public void testSuggest() throws IOException {
        //1.创建封装查询条件的对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //创建suggest
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion("my-suggest", SuggestBuilders.completionSuggestion("name").prefix("s").size(30));
        searchSourceBuilder.suggest(suggestBuilder);
        //2.构建搜索的请求对象，把searchSourceBuilder放进去
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.source(searchSourceBuilder);
        //3.发请求,得到结果
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        //4.解析结果
        Suggest suggest = response.getSuggest();
        //4.1根据自动补全的名称获取suggest结果
        CompletionSuggestion suggestion = suggest.getSuggestion("my-suggest");
        List<CompletionSuggestion.Entry.Option> options = suggestion.getOptions();
        //4.2遍历结果
        for (CompletionSuggestion.Entry.Option option : options) {
            Text text = option.getText();
            System.out.println("text = " + text);
        }
    }


    /**
     * 关闭客户端连接
     */
    @After
    public void close() throws IOException {
        client.close();
    }
}