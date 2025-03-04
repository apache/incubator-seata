package org.apache.seata.discovery.registry.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.discovery.registry.etcd3.EtcdRegistryProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class EtcdTest {
    private static final Logger logger = LoggerFactory.getLogger(EtcdTest.class);
    private static final String REGISTRY_KEY_PREFIX = "registry-seata-";
    private final static String HOST = "127.0.0.1";
    private final static int PORT = 8091;

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
            .withNodes(1)
            .build();
    private static  Client client;

    @BeforeAll
    public static void beforeAll() {
        client = Client.builder().endpoints(cluster.clientEndpoints()).build();
    }


    @Test
    public void testRegister() throws Exception {
        RegistryService registryService = new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        //1.register
        registryService.register(inetSocketAddress);
        //2.get instance information
        //GetOption getOption = GetOption.newBuilder().withPrefix(buildRegistryKeyPrefix()).build();
        long count = client.getKVClient().get(buildRegistryKeyPrefix()).get().getKvs().stream().filter(keyValue -> {
            String[] instanceInfo = keyValue.getValue().toString(StandardCharsets.UTF_8).split(":");
            return HOST.equals(instanceInfo[0]) && PORT == Integer.parseInt(instanceInfo[1]);
        }).count();
        assertThat(count).isEqualTo(1);
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
