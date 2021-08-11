package io.seata.server.storage.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * @author UmizzZ
 * @date
 */
public class ClientConnectDAO {
    public RestHighLevelClient ClientConnect(){
        final  CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,new UsernamePasswordCredentials("elastic", "elastic"));
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost",9200,"http"))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(
                                    HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder
                                        .setDefaultCredentialsProvider(credentialsProvider);
                            }
                        }));
        return client;
    }
    public void ClientShutdown(RestHighLevelClient client){
        if(client != null){
            try{
                client.close();
            }
            catch (Exception e){
                throw new ElasticsearchException(e);
            }
        }
    }
}
