package io.seata.discovery.registry.namingserver;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServlet.class);

    // post请求方法
    public static HttpResponse doPost(String url, String jsonBody) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(url);

            // 使用json格式构建请求体参数
            StringEntity entity = new StringEntity(jsonBody);

            // 设置请求头信息
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(entity);

            HttpResponse response = httpClient.execute(httpPost);

            httpClient.close();

            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // get请求方法
    public static CloseableHttpResponse doGet(String url, Map<String, String> queryParams) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            // 构建请求参数
            URIBuilder uriBuilder = new URIBuilder(url);
            if (queryParams != null) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue());
                }
            }

            HttpGet httpGet = new HttpGet(uriBuilder.build());

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5000) // 连接超时时间，单位毫秒
                    .setSocketTimeout(5000) // 读取超时时间，单位毫秒
                    .build();
            httpGet.setConfig(requestConfig);

            // 设置请求头信息
            httpGet.setHeader("Content-Type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpGet);


            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
