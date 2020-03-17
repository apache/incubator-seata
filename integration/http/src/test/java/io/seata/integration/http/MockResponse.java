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


import java.io.IOException;
import java.io.OutputStream;

/**
 * @author : wangxb
 */
public class MockResponse {

    private OutputStream outputStream;

    public MockResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(String content) throws IOException {
        StringBuffer httpResponse = new StringBuffer();
        httpResponse.append("HTTP/1.1 200 OK\n")      //按照HTTP响应报文的格式写入
                .append("Content-Type:application/json\n")
                .append("\r\n")
                .append(content);       //将页面内容写入
        outputStream.write(httpResponse.toString().getBytes());      //将文本转为字节流
        outputStream.close();
    }
}
