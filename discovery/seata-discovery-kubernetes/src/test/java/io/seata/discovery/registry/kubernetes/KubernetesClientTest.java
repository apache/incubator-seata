package io.seata.discovery.registry.kubernetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.zjsonpatch.internal.guava.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static javax.swing.UIManager.put;

/**
 * @author zhanghao
 * @date 2019/5/19 17:05
 **/
public class KubernetesClientTest {

    @ClassRule
    public static KubernetesServer mockServer = new KubernetesServer(true,true);

    private static KubernetesClient mockClient;


    private static final String SERVICE_NAME = "default";



    @Before
    public void setup() {
//        mockClient = new DefaultKubernetesClient();
        mockClient = mockServer.getClient();
        // Configure the kubernetes master url to point to the mock server
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY,
                mockClient.getConfiguration().getMasterUrl());
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY,
                "false");
    }

    @Test
    public void testClient() throws Exception {
//        EndpointAddress endpointAddress1 = new EndpointAddressBuilder().withIp("127.0.0.1").build();
//        EndpointAddress endpointAddress2 = new EndpointAddressBuilder().withIp("localhost").build();
//
//        EndpointPort endpointPort = new EndpointPortBuilder().withPort(8080).build();
//
//        EndpointSubset endpointSubset = new EndpointSubsetBuilder().withAddresses(endpointAddress1,endpointAddress2).withPorts(endpointPort).build();
//
//        Endpoints endpoints = new EndpointsBuilder().withNewMetadata().withName(SERVICE_NAME).and().addToSubsets(endpointSubset).build();
//
//        mockClient.endpoints().create(endpoints);

//


//        Pod pod = new PodBuilder().withNewMetadata().withName(SERVICE_NAME).withLabels(new HashMap<String, String>() {
//            {
//                put("app", SERVICE_NAME);
//            }
//        }).and().build();
//
//        final Service service = new ServiceBuilder().withNewMetadata().withName(SERVICE_NAME).and().withNewSpec().withSelector(new HashMap<String, String>() {
//            {
//                put("app", SERVICE_NAME);
//            }
//        }).withPorts(new ServicePortBuilder().withPort(8080).build(), new ServicePortBuilder().withPort(8081).build()).endSpec().build();
//
//        mockClient.pods().create(pod);
//        mockClient.services().create(service);

        KubernetesRegistryServiceImpl kubernetesRegistryServic =  KubernetesRegistryServiceImpl.getInstance(mockClient);

        final List<InetSocketAddress> inetSocketAddressList = kubernetesRegistryServic.lookup("my_test_tx_group");

        System.out.println(inetSocketAddressList);
    }
}
