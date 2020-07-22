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
package io.seata.integration.http;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : wangxb
 */
public class ServletMapping {

    public static List<ServletMapping> servletMappingList = new ArrayList<>();

    static {
        servletMappingList.add(new ServletMapping("/testGet", "testGet", "io.seata.integration.http.MockController"));
        servletMappingList.add(new ServletMapping("/testPost", "testPost", "io.seata.integration.http.MockController"));
        servletMappingList.add(new ServletMapping("/testException", "testException", "io.seata.integration.http.MockController"));
    }

    private String method;
    private String path;
    private String clazz;

    public ServletMapping(String path, String method, String clazz) {
        this.method = method;
        this.path = path;
        this.clazz = clazz;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }
}
