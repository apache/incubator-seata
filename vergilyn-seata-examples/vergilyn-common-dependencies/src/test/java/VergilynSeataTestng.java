import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Slf4j
public class VergilynSeataTestng {
    private HttpClient httpClient;

    private static final String USER_ID = "1";
    private static final String COMMODITY_CODE = "C201901140001";

    @BeforeTest
    public void beforeTest(){
        httpClient = HttpClients.custom()
                .setMaxConnPerRoute(20)
                .setMaxConnTotal(40)
                .build();

        System.out.println("BeforeTest >>>> ");
        printInfo();

    }

    @AfterTest
    public void afterTest(){
        System.out.println("AfterTest >>>> ");
        printInfo();

        System.out.println();
    }


    @Test
    public void createOrder() {
        String url = String.format("http://127.0.0.1:9030/order/create?userId=%s&commodityCode=%s&orderTotal=%s&orderAmount=%s",
                USER_ID, COMMODITY_CODE, "10", "400.00");
        System.out.println("request url >>>> " + url);
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet);
            String str = EntityUtils.toString(response.getEntity());
            System.out.println("`/order/create` response >>>> " +str );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printInfo() {
        try {
            HttpResponse accountResp = httpClient.execute(new HttpGet("http://127.0.0.1:9020/account/get?userId=" + USER_ID));
            System.out.println("account -> " + EntityUtils.toString(accountResp.getEntity()));
        } catch (IOException e) {
            log.error("获取account信息失败 >>>> userId: {}, message:{}", USER_ID, e.getMessage());
        }

        try {
            HttpResponse storageResp = httpClient.execute(new HttpGet("http://127.0.0.1:9040/storage/get?commodityCode=" + COMMODITY_CODE));
            System.out.println("storage -> " + EntityUtils.toString(storageResp.getEntity()));
        } catch (IOException e) {
            log.error("获取storage信息失败 >>>> commodityCode: {}, message:{}", COMMODITY_CODE, e.getMessage());
        }

        try {
            HttpResponse accountResp = httpClient.execute(new HttpGet("http://127.0.0.1:9030/order/count"));
            System.out.println("order total rows -> " + EntityUtils.toString(accountResp.getEntity()));
        } catch (IOException e) {
            log.error("获取order信息失败 >>>> message:{}", e.getMessage());
        }
    }
}
