package org.springframework.aot.hint;

import org.springframework.core.io.Resource;

public class ResourceHints {

//    public Stream<ResourcePatternHints> resourcePatternHints() {
//        return null;
//    }
//
//    public Stream<ResourceBundleHint> resourceBundleHints() {
//        return null;
//    }
//
//    public ResourceHints registerPatternIfPresent(@Nullable ClassLoader classLoader, String location,
//            Consumer<ResourcePatternHints.Builder> resourceHint) {
//        return this;
//    }
//
//    public ResourceHints registerPattern(@Nullable Consumer<ResourcePatternHints.Builder> resourceHint) {
//        ResourcePatternHints.Builder builder = new ResourcePatternHints.Builder();
//        if (resourceHint != null) {
//            resourceHint.accept(builder);
//        }
//        this.resourcePatternHints.add(builder.build());
//        return this;
//    }

    public ResourceHints registerPattern(String include) {
        return this;
    }

    public void registerResource(Resource resource) {
    }

//    public ResourceHints registerType(TypeReference type) {
//        return this;
//    }

    public ResourceHints registerType(Class<?> type) {
        return this;
    }

//    public ResourceHints registerResourceBundle(String baseName, @Nullable Consumer<ResourceBundleHint.Builder> resourceHint) {
//        return this;
//    }

    public ResourceHints registerResourceBundle(String baseName) {
        return this;
    }

}
