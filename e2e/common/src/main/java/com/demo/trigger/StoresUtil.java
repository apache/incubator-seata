package com.demo.trigger;


import org.junit.jupiter.api.extension.ExtensionContext;

public final class StoresUtil {
    public static ExtensionContext.Store store(final ExtensionContext context) {

        final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(
                context.getRequiredTestClass(),
                context.getRequiredTestMethod()
        );
        return context.getRoot().getStore(namespace);
    }

    public static <T> T get(final ExtensionContext context, final Object key, final Class<T> requiredType) {
        return store(context).get(key, requiredType);
    }

    public static void put(final ExtensionContext context, final Object key, final Object value) {
        store(context).put(key, value);
    }
}
