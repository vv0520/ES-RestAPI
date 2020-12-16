import com.alibaba.fastjson.JSON;
import com.itgeima.es.entity.Goods;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

public class TestAsync {
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

    /**
     * 异步新增
     */
    @Test
    public void testAsyncAddDocument() throws InterruptedException {
        // 准备文档,
        Goods goods = new Goods(5L, Collections.singletonList("松下电吹风"), "松下电吹风 网红电吹风", 1599L);

        // 创建请求
        IndexRequest request = new IndexRequest("goods")
                .id(goods.getId().toString())
                .source(JSON.toJSONString(goods), XContentType.JSON);

        // 执行请求，第三个参数是回调处理
        client.indexAsync(request, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
            /**
             * 执行成功时的回调，参数是IndexResponse结果
             * @param indexResponse 执行结果
             */
            @Override
            public void onResponse(IndexResponse indexResponse) {
                System.out.println("我是成功的回调！" + indexResponse);
            }

            /**
             * 执行失败时的回调，参数是异常信息
             * @param e 异常信息
             */
            @Override
            public void onFailure(Exception e) {
                System.out.println("我是失败的回调！");
                e.printStackTrace();
            }
        });

        System.out.println("我的异步方法调用完成~~");
        // 因为我们的程序结束会立即停止，接收不到回调结果，这里我们休眠一下，等待下回调结果
        Thread.sleep(2000L);
    }

    //异步删除
    @Test
    public void testAsyncDeleteDocument() throws InterruptedException {
        //1.创建请求
        DeleteRequest request = new DeleteRequest("goods", "5");
        //2.发送请求
        client.deleteAsync(request,
                RequestOptions.DEFAULT,
                new ActionListener<DeleteResponse>() {//回调处理
                    //执行成功时的回调
                    @Override
                    public void onResponse(DeleteResponse deleteResponse) {
                        System.out.println("我是成功的回调！" + deleteResponse);
                    }
                    //执行失败时的回调
                    @Override
                    public void onFailure(Exception e) {
                        System.out.println("我是失败的回调！");
                        e.printStackTrace();
                    }
                });
        System.out.println("我的异步方法调用完成~~");
        // 因为我们的程序结束会立即停止，接收不到回调结果，这里我们休眠一下，等待下回调结果
        Thread.sleep(2000L);
    }

    //异步查询
    @Test
    public void testGetDocumentByIdAsync() throws IOException, InterruptedException {
        //1.创建请求
        GetRequest getRequest = new GetRequest("goods", "1");
        //2.发送请求
        client.getAsync(getRequest, RequestOptions.DEFAULT, new ActionListener<GetResponse>() {
            //查询成功回调
            @Override
            public void onResponse(GetResponse response) {
                String source = response.getSourceAsString();
                Goods goods = JSON.parseObject(source, Goods.class);//将json反序列化
                System.out.println("查询结束，得到结果： " + goods);
            }
            //查询失败回调
            @Override
            public void onFailure(Exception e) {
                System.out.println("我是查询失败的回调！");
            }
        });
        System.out.println("我的异步方法调用完成~~");
        Thread.sleep(2000L);
    }




















    @After
    public void close() throws IOException {
        client.close();
    }
}
