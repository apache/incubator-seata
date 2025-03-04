package org.apache.seata.discovery.registry.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class EtcdTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdTest.class);
    private static final String REGISTRY_KEY_PREFIX = "registry-seata-";
    private final static String HOST = "127.0.0.1";
    private final static int PORT = 8091;

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
            .withNodes(1)
            .build();
    private static final Client client = Client.builder().endpoints(cluster.clientEndpoints()).build();

    /*
    @BeforeAll
    public static void beforeAll() {
        client = Client.builder().endpoints(cluster.clientEndpoints()).build();
    }

     */


    @Test
    public void testRegister() throws Exception {
        KV kvClient = client.getKVClient();
        ByteSequence key = ByteSequence.from("test_key".getBytes());
        ByteSequence value = ByteSequence.from("test_value".getBytes());
        kvClient.put(key, value).get();
        CompletableFuture<GetResponse> getFuture = kvClient.get(key);

// get the value from CompletableFuture
        GetResponse response = getFuture.get();
        LOGGER.info("result:==========>key:{},value:{}", response.getKvs().get(0).getKey(), response.getKvs().get(0).getValue());
        /*
        RegistryService registryService = new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        //1.register
        registryService.register(inetSocketAddress);
        //2.get instance information
        long count = client.getKVClient().get(buildRegistryKeyPrefix()).get().getKvs().stream().filter(keyValue -> {
            String[] instanceInfo = keyValue.getValue().toString(StandardCharsets.UTF_8).split(":");
            return HOST.equals(instanceInfo[0]) && PORT == Integer.parseInt(instanceInfo[1]);
        }).count();
        assertThat(count).isEqualTo(1);

         */
    }

    /**
     * build registry key prefix
     *
     * @return ByteSequence
     */
    private ByteSequence buildRegistryKeyPrefix() {
        return ByteSequence.from(REGISTRY_KEY_PREFIX, StandardCharsets.UTF_8);
    }

}
