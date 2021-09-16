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

import io.seata.core.context.RootContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : wangxb
 */
public class MockRequest {
    private String url;
    private Map<String, String> header = new HashMap<>();
    private String body;
    private String path;
    private String method = "get";

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public MockRequest(String url, Map<String, String> header, String body, String path,String method) {

        this.url = url;
        this.header = header;
        this.body = body;
        this.path = path;
        this.method = method;
    }

    public MockRequest(InputStream inputStream) throws IOException {
        String httpRequest = "";
        byte[] httpRequestBytes = new byte[2048];
        int length = 0;
        if ((length = inputStream.read(httpRequestBytes)) > 0) {
            httpRequest = new String(httpRequestBytes, 0, length);
        }

        String httpHead = httpRequest.split("\n")[0];
        url = httpHead.split("\\s")[1];
        String xid = httpRequest.split("\\n")[1];
        if (xid.startsWith(RootContext.KEY_XID)) {
            xid = xid.split(RootContext.KEY_XID + ":")[1].trim();
        }
        header.put(RootContext.KEY_XID, xid);

        path = url.split("\\?")[0];
        if (httpRequest.startsWith("POST")) {
            body = httpRequest.split("\\n")[9];
            method = "post";
        }
    }

    public String getUrl() {
        return url;
    }


}
