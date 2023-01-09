/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.aot.hint;

import org.springframework.core.io.Resource;

/**
 * ResourceHints's fake
 *
 * @author wang.liang
 */
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
