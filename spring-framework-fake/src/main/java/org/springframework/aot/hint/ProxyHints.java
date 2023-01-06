package org.springframework.aot.hint;

public class ProxyHints {

//    public Stream<JdkProxyHint> jdkProxyHints() {
//        return null;
//    }
//
//    public ProxyHints registerJdkProxy(Consumer<JdkProxyHint.Builder> jdkProxyHint) {
//        return this;
//    }

    public ProxyHints registerJdkProxy(TypeReference... proxiedInterfaces) {
        return this;
    }

    public ProxyHints registerJdkProxy(Class<?>... proxiedInterfaces) {
        return this;
    }

}
