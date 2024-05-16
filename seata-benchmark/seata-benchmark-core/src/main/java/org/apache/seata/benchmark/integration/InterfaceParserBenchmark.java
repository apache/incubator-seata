package org.apache.seata.benchmark.integration;

import org.apache.seata.benchmark.model.TccAction;
import org.apache.seata.benchmark.model.TccActionImpl;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

public class InterfaceParserBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.All)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void tccProxyInvocationHandler() throws Exception {
        TccActionImpl target = new TccActionImpl();
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(target, TccAction.class.getName());
        assert proxyInvocationHandler != null;
    }

//    public static void main(String[] args) throws Exception {
//        TccActionImpl target = new TccActionImpl();
//        //1、TODO spi加载逻辑，目前只加载seata-all的包
//        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(target, TccAction.class.getName());
//        System.out.println(proxyInvocationHandler);
//    }
}