/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * ------------------------------------------------------------------------
 * This file contains code originally from the [Model Context Protocol Java SDK],
 * which is licensed under the MIT License.
 *
 * Modifications made by [Seata]:
 *   - Adapted code from Java 17 features to Java 8 compatible syntax
 *   - [Optional: record class to static immutable class]
 *
 * The original MIT license text is reproduced below:
 * ------------------------------------------------------------------------
 */

/*
 * MIT License
 * Copyright (c) 2025 the original author or authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Based on the <a href="http://www.jsonrpc.org/specification">JSON-RPC 2.0
 * specification</a> and the <a href=
 * "https://github.com/modelcontextprotocol/specification/blob/main/schema/schema.ts">Model
 * Context Protocol Schema</a>.
 *
 * @author Christian Tzolov
 */
public final class McpSchema {

    private static final Logger logger = LoggerFactory.getLogger(McpSchema.class);

    private McpSchema() {}

    public static final String JSONRPC_VERSION = "2.0";

    public static final String FIRST_PAGE = null;

    // ---------------------------
    // Method Names
    // ---------------------------

    // Lifecycle Methods
    public static final String METHOD_INITIALIZE = "initialize";

    public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

    public static final String METHOD_PING = "ping";

    // Tool Methods
    public static final String METHOD_TOOLS_LIST = "tools/list";

    public static final String METHOD_TOOLS_CALL = "tools/call";

    public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

    // Logging Methods
    public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

    // Roots Methods
    public static final String METHOD_ROOTS_LIST = "roots/list";

    public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ---------------------------
    // JSON-RPC Error Codes
    // ---------------------------
    /**
     * Standard error codes used in MCP JSON-RPC responses.
     */
    public static final class ErrorCodes {

        /**
         * The method does not exist / is not available.
         */
        public static final int METHOD_NOT_FOUND = -32601;

        /**
         * Internal JSON-RPC error.
         */
        public static final int INTERNAL_ERROR = -32603;
    }

    public interface Request {
        Map<String, Object> meta();

        default String progressToken() {
            if (meta() != null && meta().containsKey("progressToken")) {
                return meta().get("progressToken").toString();
            }
            return null;
        }
    }

    public interface Result {

        Map<String, Object> meta();
    }

    public interface Notification {

        Map<String, Object> meta();
    }

    private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF =
            new TypeReference<HashMap<String, Object>>() {};

    public static JSONRPCMessage deserializeJsonRpcMessage(ObjectMapper objectMapper, String jsonText)
            throws IOException {

        logger.debug("Received JSON message: {}", jsonText);

        HashMap<String, Object> map = objectMapper.readValue(jsonText, MAP_TYPE_REF);

        // Determine message type based on specific JSON structure
        if (map.containsKey("method") && map.containsKey("id")) {
            return objectMapper.convertValue(map, JSONRPCRequest.class);
        } else if (map.containsKey("method") && !map.containsKey("id")) {
            return objectMapper.convertValue(map, JSONRPCNotification.class);
        } else if (map.containsKey("result") || map.containsKey("error")) {
            return objectMapper.convertValue(map, JSONRPCResponse.class);
        }

        throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + jsonText);
    }

    // ---------------------------
    // JSON-RPC Message Types
    // ---------------------------
    public interface JSONRPCMessage {
        String getJsonrpc();
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCRequest implements JSONRPCMessage {
        @JsonProperty("jsonrpc")
        String jsonrpc;

        @JsonProperty("method")
        String method;

        @JsonProperty("id")
        Object id;

        @JsonProperty("params")
        Object params;

        public JSONRPCRequest(String jsonrpc, String method, Object id, Object params) {
            this.jsonrpc = jsonrpc;
            this.method = method;
            this.id = id;
            this.params = params;
        }

        public JSONRPCRequest() {}

        @Override
        public String getJsonrpc() {
            return jsonrpc;
        }

        public void setJsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public Object getParams() {
            return params;
        }

        public void setParams(Object params) {
            this.params = params;
        }

        @Override
        public String toString() {
            return "JSONRPCRequest{" + "jsonrpc='"
                    + jsonrpc + '\'' + ", method='"
                    + method + '\'' + ", id="
                    + id + ", params="
                    + params + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            JSONRPCRequest that = (JSONRPCRequest) o;
            return Objects.equals(jsonrpc, that.jsonrpc)
                    && Objects.equals(method, that.method)
                    && Objects.equals(id, that.id)
                    && Objects.equals(params, that.params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jsonrpc, method, id, params);
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCNotification implements JSONRPCMessage {
        @JsonProperty("jsonrpc")
        String jsonrpc;

        @JsonProperty("method")
        String method;

        @JsonProperty("params")
        Object params;

        public JSONRPCNotification() {}

        public JSONRPCNotification(String jsonrpc, String method, Object params) {
            this.jsonrpc = jsonrpc;
            this.method = method;
            this.params = params;
        }

        @Override
        public String getJsonrpc() {
            return jsonrpc;
        }

        public void setJsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Object getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            JSONRPCNotification that = (JSONRPCNotification) o;
            return Objects.equals(jsonrpc, that.jsonrpc)
                    && Objects.equals(method, that.method)
                    && Objects.equals(params, that.params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jsonrpc, method, params);
        }

        @Override
        public String toString() {
            return "JSONRPCNotification{" + "jsonrpc='"
                    + jsonrpc + '\'' + ", method='"
                    + method + '\'' + ", params="
                    + params + '}';
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCResponse implements JSONRPCMessage {
        @JsonProperty("jsonrpc")
        String jsonrpc;

        @JsonProperty("id")
        Object id;

        @JsonProperty("result")
        Object result;

        @JsonProperty("error")
        JSONRPCError error;

        public JSONRPCResponse(String jsonrpc, Object id, Object result, JSONRPCError error) {
            this.jsonrpc = jsonrpc;
            this.id = id;
            this.result = result;
            this.error = error;
        }

        public JSONRPCResponse() {}

        @Override
        public String getJsonrpc() {
            return jsonrpc;
        }

        public void setJsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
        }

        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public JSONRPCError getError() {
            return error;
        }

        public void setError(JSONRPCError error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return "JSONRPCResponse{" + "jsonrpc='"
                    + jsonrpc + '\'' + ", id="
                    + id + ", result="
                    + result + ", error="
                    + error + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            JSONRPCResponse that = (JSONRPCResponse) o;
            return Objects.equals(jsonrpc, that.jsonrpc)
                    && Objects.equals(id, that.id)
                    && Objects.equals(result, that.result)
                    && Objects.equals(error, that.error);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jsonrpc, id, result, error);
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class JSONRPCError {
            @JsonProperty("code")
            int code;

            @JsonProperty("message")
            String message;

            @JsonProperty("data")
            Object data;

            public JSONRPCError(int code, String message, Object data) {
                this.code = code;
                this.message = message;
                this.data = data;
            }

            public JSONRPCError() {}

            public int getCode() {
                return code;
            }

            public void setCode(int code) {
                this.code = code;
            }

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

            public Object getData() {
                return data;
            }

            public void setData(Object data) {
                this.data = data;
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                JSONRPCError that = (JSONRPCError) o;
                return code == that.code && Objects.equals(message, that.message) && Objects.equals(data, that.data);
            }

            @Override
            public int hashCode() {
                return Objects.hash(code, message, data);
            }

            @Override
            public String toString() {
                return "JSONRPCError{" + "code=" + code + ", message='" + message + '\'' + ", data=" + data + '}';
            }
        }
    } // @formatter:on

    // ---------------------------
    // Initialization
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InitializeRequest implements Request {
        @JsonProperty("protocolVersion")
        String protocolVersion;

        @JsonProperty("capabilities")
        ClientCapabilities capabilities;

        @JsonProperty("clientInfo")
        Implementation clientInfo;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        public InitializeRequest(String protocolVersion, ClientCapabilities capabilities, Implementation clientInfo) {
            this.protocolVersion = protocolVersion;
            this.capabilities = capabilities;
            this.clientInfo = clientInfo;
            this.meta = null;
        }

        public InitializeRequest() {}

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public void setProtocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        public ClientCapabilities getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(ClientCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        public Implementation getClientInfo() {
            return clientInfo;
        }

        public void setClientInfo(Implementation clientInfo) {
            this.clientInfo = clientInfo;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        @Override
        public String toString() {
            return "InitializeRequest{" + "protocolVersion='"
                    + protocolVersion + '\'' + ", capabilities="
                    + capabilities + ", clientInfo="
                    + clientInfo + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            InitializeRequest that = (InitializeRequest) o;
            return Objects.equals(protocolVersion, that.protocolVersion)
                    && Objects.equals(capabilities, that.capabilities)
                    && Objects.equals(clientInfo, that.clientInfo)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(protocolVersion, capabilities, clientInfo, meta);
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InitializeResult implements Result {
        @JsonProperty("protocolVersion")
        String protocolVersion;

        @JsonProperty("capabilities")
        ServerCapabilities capabilities;

        @JsonProperty("serverInfo")
        Implementation serverInfo;

        @JsonProperty("instructions")
        String instructions;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        public InitializeResult() {}

        @Override
        public String toString() {
            return "InitializeResult{" + "protocolVersion='"
                    + protocolVersion + '\'' + ", capabilities="
                    + capabilities + ", serverInfo="
                    + serverInfo + ", instructions='"
                    + instructions + '\'' + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            InitializeResult that = (InitializeResult) o;
            return Objects.equals(protocolVersion, that.protocolVersion)
                    && Objects.equals(capabilities, that.capabilities)
                    && Objects.equals(serverInfo, that.serverInfo)
                    && Objects.equals(instructions, that.instructions)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(protocolVersion, capabilities, serverInfo, instructions, meta);
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public void setProtocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        public ServerCapabilities getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(ServerCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        public Implementation getServerInfo() {
            return serverInfo;
        }

        public void setServerInfo(Implementation serverInfo) {
            this.serverInfo = serverInfo;
        }

        public String getInstructions() {
            return instructions;
        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public InitializeResult(
                String protocolVersion,
                ServerCapabilities capabilities,
                Implementation serverInfo,
                String instructions) {
            this.protocolVersion = protocolVersion;
            this.capabilities = capabilities;
            this.serverInfo = serverInfo;
            this.instructions = instructions;
            this.meta = null;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientCapabilities {
        @JsonProperty("experimental")
        Map<String, Object> experimental;

        @JsonProperty("roots")
        RootCapabilities roots;

        @JsonProperty("sampling")
        Sampling sampling;

        @JsonProperty("elicitation")
        Elicitation elicitation;

        public ClientCapabilities(
                Map<String, Object> experimental, RootCapabilities roots, Sampling sampling, Elicitation elicitation) {
            this.experimental = experimental;
            this.roots = roots;
            this.sampling = sampling;
            this.elicitation = elicitation;
        }

        public ClientCapabilities() {}

        public Map<String, Object> getExperimental() {
            return experimental;
        }

        public void setExperimental(Map<String, Object> experimental) {
            this.experimental = experimental;
        }

        public RootCapabilities getRoots() {
            return roots;
        }

        public void setRoots(RootCapabilities roots) {
            this.roots = roots;
        }

        public Sampling getSampling() {
            return sampling;
        }

        public void setSampling(Sampling sampling) {
            this.sampling = sampling;
        }

        public Elicitation getElicitation() {
            return elicitation;
        }

        public void setElicitation(Elicitation elicitation) {
            this.elicitation = elicitation;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ClientCapabilities that = (ClientCapabilities) o;
            return Objects.equals(experimental, that.experimental)
                    && Objects.equals(roots, that.roots)
                    && Objects.equals(sampling, that.sampling);
        }

        @Override
        public int hashCode() {
            return Objects.hash(experimental, roots, sampling);
        }

        @Override
        public String toString() {
            return "ClientCapabilities{" + "experimental="
                    + experimental + ", roots="
                    + roots + ", sampling="
                    + sampling + '}';
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class Elicitation {}

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RootCapabilities {
            @JsonProperty("listChanged")
            Boolean listChanged;

            public RootCapabilities() {}

            public RootCapabilities(Boolean listChanged) {
                this.listChanged = listChanged;
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                RootCapabilities that = (RootCapabilities) o;
                return Objects.equals(listChanged, that.listChanged);
            }

            @Override
            public String toString() {
                return "RootCapabilities{" + "listChanged=" + listChanged + '}';
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(listChanged);
            }

            public Boolean getListChanged() {
                return listChanged;
            }

            public void setListChanged(Boolean listChanged) {
                this.listChanged = listChanged;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static class Sampling {}

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Map<String, Object> experimental;

            private RootCapabilities roots;

            private Sampling sampling;

            private Elicitation elicitation;

            public Builder experimental(Map<String, Object> experimental) {
                this.experimental = experimental;
                return this;
            }

            public Builder roots(Boolean listChanged) {
                this.roots = new RootCapabilities(listChanged);
                return this;
            }

            public Builder sampling() {
                this.sampling = new Sampling();
                return this;
            }

            public Builder elicitation() {
                this.elicitation = new Elicitation();
                return this;
            }

            public ClientCapabilities build() {
                return new ClientCapabilities(experimental, roots, sampling, elicitation);
            }
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ServerCapabilities {
        @JsonProperty("completions")
        private final CompletionCapabilities completions;

        @JsonProperty("experimental")
        private final Map<String, Object> experimental;

        @JsonProperty("logging")
        private final LoggingCapabilities logging;

        @JsonProperty("prompts")
        private final PromptCapabilities prompts;

        @JsonProperty("resources")
        private final ResourceCapabilities resources;

        @JsonProperty("tools")
        private final ToolCapabilities tools;

        @Override
        public String toString() {
            return "ServerCapabilities{" + "completions="
                    + completions + ", experimental="
                    + experimental + ", logging="
                    + logging + ", prompts="
                    + prompts + ", resources="
                    + resources + ", tools="
                    + tools + '}';
        }

        @JsonCreator
        public ServerCapabilities(
                @JsonProperty("completions") CompletionCapabilities completions,
                @JsonProperty("experimental") Map<String, Object> experimental,
                @JsonProperty("logging") LoggingCapabilities logging,
                @JsonProperty("prompts") PromptCapabilities prompts,
                @JsonProperty("resources") ResourceCapabilities resources,
                @JsonProperty("tools") ToolCapabilities tools) {
            this.completions = completions;
            this.experimental = experimental;
            this.logging = logging;
            this.prompts = prompts;
            this.resources = resources;
            this.tools = tools;
        }

        public CompletionCapabilities completions() {
            return completions;
        }

        public Map<String, Object> experimental() {
            return experimental;
        }

        public LoggingCapabilities logging() {
            return logging;
        }

        public PromptCapabilities prompts() {
            return prompts;
        }

        public ResourceCapabilities resources() {
            return resources;
        }

        public ToolCapabilities tools() {
            return tools;
        }

        public Builder mutate() {
            Builder builder = new Builder();
            builder.completions = this.completions;
            builder.experimental = this.experimental;
            builder.logging = this.logging;
            builder.prompts = this.prompts;
            builder.resources = this.resources;
            builder.tools = this.tools;
            return builder;
        }

        public static Builder builder() {
            return new Builder();
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static final class CompletionCapabilities {
            public CompletionCapabilities() {}
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static final class LoggingCapabilities {
            public LoggingCapabilities() {}
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static final class PromptCapabilities {
            @JsonProperty("listChanged")
            private final Boolean listChanged;

            @JsonCreator
            public PromptCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
                this.listChanged = listChanged;
            }

            @Override
            public String toString() {
                return "PromptCapabilities{" + "listChanged=" + listChanged + '}';
            }

            public Boolean listChanged() {
                return listChanged;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static final class ResourceCapabilities {
            @JsonProperty("subscribe")
            private final Boolean subscribe;

            @JsonProperty("listChanged")
            private final Boolean listChanged;

            @JsonCreator
            public ResourceCapabilities(
                    @JsonProperty("subscribe") Boolean subscribe, @JsonProperty("listChanged") Boolean listChanged) {
                this.subscribe = subscribe;
                this.listChanged = listChanged;
            }

            @Override
            public String toString() {
                return "ResourceCapabilities{" + "subscribe=" + subscribe + ", listChanged=" + listChanged + '}';
            }

            public Boolean subscribe() {
                return subscribe;
            }

            public Boolean listChanged() {
                return listChanged;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        public static final class ToolCapabilities {
            @JsonProperty("listChanged")
            private final Boolean listChanged;

            @Override
            public String toString() {
                return "ToolCapabilities{" + "listChanged=" + listChanged + '}';
            }

            @JsonCreator
            public ToolCapabilities(@JsonProperty("listChanged") Boolean listChanged) {
                this.listChanged = listChanged;
            }

            public Boolean listChanged() {
                return listChanged;
            }
        }

        public static class Builder {
            private CompletionCapabilities completions;
            private Map<String, Object> experimental;
            private LoggingCapabilities logging = new LoggingCapabilities();
            private PromptCapabilities prompts;
            private ResourceCapabilities resources;
            private ToolCapabilities tools;

            public Builder completions() {
                this.completions = new CompletionCapabilities();
                return this;
            }

            public Builder experimental(Map<String, Object> experimental) {
                this.experimental = experimental;
                return this;
            }

            public Builder logging() {
                this.logging = new LoggingCapabilities();
                return this;
            }

            public Builder prompts(Boolean listChanged) {
                this.prompts = new PromptCapabilities(listChanged);
                return this;
            }

            public Builder resources(Boolean subscribe, Boolean listChanged) {
                this.resources = new ResourceCapabilities(subscribe, listChanged);
                return this;
            }

            public Builder tools(Boolean listChanged) {
                this.tools = new ToolCapabilities(listChanged);
                return this;
            }

            public ServerCapabilities build() {
                return new ServerCapabilities(completions, experimental, logging, prompts, resources, tools);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Implementation {
        @JsonProperty("name")
        String name;

        @JsonProperty("title")
        String title;

        @JsonProperty("version")
        String version;

        public Implementation() {}

        public Implementation(String name, String version) {
            this.name = name;
            this.version = version;
            this.title = null;
        }

        @Override
        public String toString() {
            return "Implementation{" + "name='"
                    + name + '\'' + ", title='"
                    + title + '\'' + ", version='"
                    + version + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Implementation that = (Implementation) o;
            return Objects.equals(name, that.name)
                    && Objects.equals(title, that.title)
                    && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, title, version);
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    } // @formatter:on

    // Existing Enums and Base Types (from previous implementation)
    public enum Role { // @formatter:off
        @JsonProperty("user")
        USER,
        @JsonProperty("assistant")
        ASSISTANT
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Annotations {
        @JsonProperty("audience")
        List<Role> audience;

        @JsonProperty("priority")
        Double priority;

        public Annotations() {}

        public Annotations(Double priority, List<Role> audience) {
            this.priority = priority;
            this.audience = audience;
        }

        @Override
        public String toString() {
            return "Annotations{" + "audience=" + audience + ", priority=" + priority + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Annotations that = (Annotations) o;
            return Objects.equals(audience, that.audience) && Objects.equals(priority, that.priority);
        }

        @Override
        public int hashCode() {
            return Objects.hash(audience, priority);
        }

        public List<Role> getAudience() {
            return audience;
        }

        public void setAudience(List<Role> audience) {
            this.audience = audience;
        }

        public Double getPriority() {
            return priority;
        }

        public void setPriority(Double priority) {
            this.priority = priority;
        }
    } // @formatter:on

    // ---------------------------
    // Tool Interfaces
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListToolsResult implements Result {
        @JsonProperty("tools")
        List<Tool> tools;

        @JsonProperty("nextCursor")
        String nextCursor;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ListToolsResult that = (ListToolsResult) o;
            return Objects.equals(tools, that.tools)
                    && Objects.equals(nextCursor, that.nextCursor)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tools, nextCursor, meta);
        }

        @Override
        public String toString() {
            return "ListToolsResult{" + "tools="
                    + tools + ", nextCursor='"
                    + nextCursor + '\'' + ", meta="
                    + meta + '}';
        }

        public List<Tool> getTools() {
            return tools;
        }

        public void setTools(List<Tool> tools) {
            this.tools = tools;
        }

        public String getNextCursor() {
            return nextCursor;
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public ListToolsResult(List<Tool> tools, String nextCursor) {
            this.tools = tools;
            this.nextCursor = nextCursor;
        }

        public ListToolsResult() {}

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JsonSchema {
        @JsonProperty("type")
        String type;

        @JsonProperty("properties")
        Map<String, Object> properties;

        @JsonProperty("required")
        List<String> required;

        @JsonProperty("additionalProperties")
        Boolean additionalProperties;

        @JsonProperty("$defs")
        Map<String, Object> defs;

        @JsonProperty("definitions")
        Map<String, Object> definitions;

        @Override
        public String toString() {
            return "JsonSchema{" + "type='"
                    + type + '\'' + ", properties="
                    + properties + ", required="
                    + required + ", additionalProperties="
                    + additionalProperties + ", defs="
                    + defs + ", definitions="
                    + definitions + '}';
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public List<String> getRequired() {
            return required;
        }

        public void setRequired(List<String> required) {
            this.required = required;
        }

        public Boolean getAdditionalProperties() {
            return additionalProperties;
        }

        public void setAdditionalProperties(Boolean additionalProperties) {
            this.additionalProperties = additionalProperties;
        }

        public Map<String, Object> getDefs() {
            return defs;
        }

        public void setDefs(Map<String, Object> defs) {
            this.defs = defs;
        }

        public Map<String, Object> getDefinitions() {
            return definitions;
        }

        public void setDefinitions(Map<String, Object> definitions) {
            this.definitions = definitions;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            JsonSchema schema = (JsonSchema) o;
            return Objects.equals(type, schema.type)
                    && Objects.equals(properties, schema.properties)
                    && Objects.equals(required, schema.required)
                    && Objects.equals(additionalProperties, schema.additionalProperties)
                    && Objects.equals(defs, schema.defs)
                    && Objects.equals(definitions, schema.definitions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, properties, required, additionalProperties, defs, definitions);
        }

        public JsonSchema() {}

        public JsonSchema(
                String type,
                Map<String, Object> properties,
                List<String> required,
                Boolean additionalProperties,
                Map<String, Object> defs,
                Map<String, Object> definitions) {
            this.type = type;
            this.properties = properties;
            this.required = required;
            this.additionalProperties = additionalProperties;
            this.defs = defs;
            this.definitions = definitions;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolAnnotations {
        @JsonProperty("title")
        String title;

        @JsonProperty("readOnlyHint")
        Boolean readOnlyHint;

        @JsonProperty("destructiveHint")
        Boolean destructiveHint;

        @JsonProperty("idempotentHint")
        Boolean idempotentHint;

        @JsonProperty("openWorldHint")
        Boolean openWorldHint;

        @JsonProperty("returnDirect")
        Boolean returnDirect;

        @Override
        public String toString() {
            return "ToolAnnotations{" + "title='"
                    + title + '\'' + ", readOnlyHint="
                    + readOnlyHint + ", destructiveHint="
                    + destructiveHint + ", idempotentHint="
                    + idempotentHint + ", openWorldHint="
                    + openWorldHint + ", returnDirect="
                    + returnDirect + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ToolAnnotations that = (ToolAnnotations) o;
            return Objects.equals(title, that.title)
                    && Objects.equals(readOnlyHint, that.readOnlyHint)
                    && Objects.equals(destructiveHint, that.destructiveHint)
                    && Objects.equals(idempotentHint, that.idempotentHint)
                    && Objects.equals(openWorldHint, that.openWorldHint)
                    && Objects.equals(returnDirect, that.returnDirect);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, readOnlyHint, destructiveHint, idempotentHint, openWorldHint, returnDirect);
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Boolean getReadOnlyHint() {
            return readOnlyHint;
        }

        public void setReadOnlyHint(Boolean readOnlyHint) {
            this.readOnlyHint = readOnlyHint;
        }

        public Boolean getDestructiveHint() {
            return destructiveHint;
        }

        public void setDestructiveHint(Boolean destructiveHint) {
            this.destructiveHint = destructiveHint;
        }

        public Boolean getIdempotentHint() {
            return idempotentHint;
        }

        public void setIdempotentHint(Boolean idempotentHint) {
            this.idempotentHint = idempotentHint;
        }

        public Boolean getOpenWorldHint() {
            return openWorldHint;
        }

        public void setOpenWorldHint(Boolean openWorldHint) {
            this.openWorldHint = openWorldHint;
        }

        public Boolean getReturnDirect() {
            return returnDirect;
        }

        public void setReturnDirect(Boolean returnDirect) {
            this.returnDirect = returnDirect;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tool {
        @JsonProperty("name")
        String name;

        @JsonProperty("title")
        String title;

        @JsonProperty("description")
        String description;

        @JsonProperty("inputSchema")
        JsonSchema inputSchema;

        @JsonProperty("outputSchema")
        Map<String, Object> outputSchema;

        @JsonProperty("annotations")
        ToolAnnotations annotations;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "Tool{" + "name='"
                    + name + '\'' + ", title='"
                    + title + '\'' + ", description='"
                    + description + '\'' + ", inputSchema="
                    + inputSchema + ", outputSchema="
                    + outputSchema + ", annotations="
                    + annotations + ", meta="
                    + meta + '}';
        }

        public Tool(
                String name,
                String title,
                String description,
                JsonSchema inputSchema,
                Map<String, Object> outputSchema,
                ToolAnnotations annotations,
                Map<String, Object> meta) {
            this.name = name;
            this.title = title;
            this.description = description;
            this.inputSchema = inputSchema;
            this.outputSchema = outputSchema;
            this.annotations = annotations;
            this.meta = meta;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public JsonSchema getInputSchema() {
            return inputSchema;
        }

        public void setInputSchema(JsonSchema inputSchema) {
            this.inputSchema = inputSchema;
        }

        public Map<String, Object> getOutputSchema() {
            return outputSchema;
        }

        public void setOutputSchema(Map<String, Object> outputSchema) {
            this.outputSchema = outputSchema;
        }

        public ToolAnnotations getAnnotations() {
            return annotations;
        }

        public void setAnnotations(ToolAnnotations annotations) {
            this.annotations = annotations;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public Tool(String name, String description, JsonSchema inputSchema, ToolAnnotations annotations) {
            this(name, null, description, inputSchema, null, annotations, null);
        }

        public Tool(String name, String description, String inputSchema) {
            this(name, null, description, parseSchema(inputSchema), null, null, null);
        }

        public Tool(String name, String description, String schema, ToolAnnotations annotations) {
            this(name, null, description, parseSchema(schema), null, annotations, null);
        }

        public Tool(
                String name, String description, String inputSchema, String outputSchema, ToolAnnotations annotations) {
            this(name, null, description, parseSchema(inputSchema), schemaToMap(outputSchema), annotations, null);
        }

        public Tool(
                String name,
                String title,
                String description,
                String inputSchema,
                String outputSchema,
                ToolAnnotations annotations) {
            this(name, title, description, parseSchema(inputSchema), schemaToMap(outputSchema), annotations, null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private String name;

            private String title;

            private String description;

            private JsonSchema inputSchema;

            private Map<String, Object> outputSchema;

            private ToolAnnotations annotations;

            private Map<String, Object> meta;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder inputSchema(JsonSchema inputSchema) {
                this.inputSchema = inputSchema;
                return this;
            }

            public Builder inputSchema(String inputSchema) {
                this.inputSchema = parseSchema(inputSchema);
                return this;
            }

            public Builder outputSchema(Map<String, Object> outputSchema) {
                this.outputSchema = outputSchema;
                return this;
            }

            public Builder outputSchema(String outputSchema) {
                this.outputSchema = schemaToMap(outputSchema);
                return this;
            }

            public Builder annotations(ToolAnnotations annotations) {
                this.annotations = annotations;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public Tool build() {
                Assert.hasText(name, "name must not be empty");
                return new Tool(name, title, description, inputSchema, outputSchema, annotations, meta);
            }
        }
    } // @formatter:on

    private static Map<String, Object> schemaToMap(String schema) {
        try {
            return OBJECT_MAPPER.readValue(schema, MAP_TYPE_REF);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid schema: " + schema, e);
        }
    }

    private static JsonSchema parseSchema(String schema) {
        try {
            return OBJECT_MAPPER.readValue(schema, JsonSchema.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid schema: " + schema, e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallToolRequest implements Request {
        @JsonProperty("name")
        String name;

        @JsonProperty("arguments")
        Map<String, Object> arguments;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        public CallToolRequest(String name, Map<String, Object> arguments, Map<String, Object> meta) {
            this.name = name;
            this.arguments = arguments;
            this.meta = meta;
        }

        public CallToolRequest() {}

        public CallToolRequest(String name, String jsonArguments) {
            this(name, parseJsonArguments(jsonArguments), null);
        }

        public CallToolRequest(String name, Map<String, Object> arguments) {
            this(name, arguments, null);
        }

        private static Map<String, Object> parseJsonArguments(String jsonArguments) {
            try {
                return OBJECT_MAPPER.readValue(jsonArguments, MAP_TYPE_REF);
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        @Override
        public String toString() {
            return "CallToolRequest{" + "name='" + name + '\'' + ", arguments=" + arguments + ", meta=" + meta + '}';
        }

        public static class Builder {

            private String name;

            private Map<String, Object> arguments;

            private Map<String, Object> meta;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder arguments(Map<String, Object> arguments) {
                this.arguments = arguments;
                return this;
            }

            public Builder arguments(String jsonArguments) {
                this.arguments = parseJsonArguments(jsonArguments);
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public Builder progressToken(String progressToken) {
                if (this.meta == null) {
                    this.meta = new HashMap<>();
                }
                this.meta.put("progressToken", progressToken);
                return this;
            }

            public CallToolRequest build() {
                Assert.hasText(name, "name must not be empty");
                return new CallToolRequest(name, arguments, meta);
            }
        }
    } // @formatter:off

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallToolResult implements Result {
        @JsonProperty("content")
        List<Content> content;

        @JsonProperty("isError")
        Boolean isError;

        @JsonProperty("structuredContent")
        Map<String, Object> structuredContent;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "CallToolResult{" + "content="
                    + content + ", isError="
                    + isError + ", structuredContent="
                    + structuredContent + ", meta="
                    + meta + '}';
        }

        public List<Content> getContent() {
            return content;
        }

        public void setContent(List<Content> content) {
            this.content = content;
        }

        public Boolean getError() {
            return isError;
        }

        public void setError(Boolean error) {
            isError = error;
        }

        public Map<String, Object> getStructuredContent() {
            return structuredContent;
        }

        public void setStructuredContent(Map<String, Object> structuredContent) {
            this.structuredContent = structuredContent;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public CallToolResult(
                List<Content> content,
                Boolean isError,
                Map<String, Object> structuredContent,
                Map<String, Object> meta) {
            this.content = content;
            this.isError = isError;
            this.structuredContent = structuredContent;
            this.meta = meta;
        }

        // backwards compatibility constructor
        public CallToolResult(List<Content> content, Boolean isError) {
            this(content, isError, null, null);
        }

        // backwards compatibility constructor
        public CallToolResult(List<Content> content, Boolean isError, Map<String, Object> structuredContent) {
            this(content, isError, structuredContent, null);
        }

        public CallToolResult(String content, Boolean isError) {
            this(Collections.singletonList(new TextContent(content)), isError, null);
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        /**
         * Builder for {@link CallToolResult}.
         */
        public static class Builder {

            private List<Content> content = new ArrayList<>();

            private Boolean isError = false;

            private Map<String, Object> structuredContent;

            private Map<String, Object> meta;

            public Builder content(List<Content> content) {
                Assert.notNull(content, "content must not be null");
                this.content = content;
                return this;
            }

            public Builder structuredContent(Map<String, Object> structuredContent) {
                Assert.notNull(structuredContent, "structuredContent must not be null");
                this.structuredContent = structuredContent;
                return this;
            }

            public Builder structuredContent(String structuredContent) {
                Assert.hasText(structuredContent, "structuredContent must not be empty");
                try {
                    this.structuredContent = OBJECT_MAPPER.readValue(structuredContent, MAP_TYPE_REF);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Invalid structured content: " + structuredContent, e);
                }
                return this;
            }

            public Builder textContent(List<String> textContent) {
                Assert.notNull(textContent, "textContent must not be null");
                textContent.stream().map(TextContent::new).forEach(this.content::add);
                return this;
            }

            public Builder addContent(Content contentItem) {
                Assert.notNull(contentItem, "contentItem must not be null");
                if (this.content == null) {
                    this.content = new ArrayList<>();
                }
                this.content.add(contentItem);
                return this;
            }

            public Builder addTextContent(String text) {
                Assert.notNull(text, "text must not be null");
                return addContent(new TextContent(text));
            }

            public Builder isError(Boolean isError) {
                Assert.notNull(isError, "isError must not be null");
                this.isError = isError;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public CallToolResult build() {
                return new CallToolResult(content, isError, structuredContent, meta);
            }
        }
    } // @formatter:on

    // ---------------------------
    // Pagination Interfaces
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaginatedRequest implements Request {
        @JsonProperty("cursor")
        String cursor;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        public PaginatedRequest(String cursor, Map<String, Object> meta) {
            this.cursor = cursor;
            this.meta = meta;
        }

        public String getCursor() {
            return cursor;
        }

        public void setCursor(String cursor) {
            this.cursor = cursor;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PaginatedRequest that = (PaginatedRequest) o;
            return Objects.equals(cursor, that.cursor) && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cursor, meta);
        }

        @Override
        public String toString() {
            return "PaginatedRequest{" + "cursor='" + cursor + '\'' + ", meta=" + meta + '}';
        }

        public PaginatedRequest(String cursor) {
            this(cursor, null);
        }

        /**
         * Creates a new paginated request with an empty cursor.
         */
        public PaginatedRequest() {
            this(null);
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaginatedResult {
        @JsonProperty("nextCursor")
        String nextCursor;

        @Override
        public String toString() {
            return "PaginatedResult{" + "nextCursor='" + nextCursor + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PaginatedResult that = (PaginatedResult) o;
            return Objects.equals(nextCursor, that.nextCursor);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(nextCursor);
        }

        public String getNextCursor() {
            return nextCursor;
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        public PaginatedResult() {}

        public PaginatedResult(String nextCursor) {
            this.nextCursor = nextCursor;
        }
    }

    // ---------------------------
    // Logging
    // ---------------------------
    public enum LoggingLevel { // @formatter:off
        @JsonProperty("debug")
        DEBUG(0),
        @JsonProperty("info")
        INFO(1),
        @JsonProperty("notice")
        NOTICE(2),
        @JsonProperty("warning")
        WARNING(3),
        @JsonProperty("error")
        ERROR(4),
        @JsonProperty("critical")
        CRITICAL(5),
        @JsonProperty("alert")
        ALERT(6),
        @JsonProperty("emergency")
        EMERGENCY(7);

        private final int level;

        LoggingLevel(int level) {
            this.level = level;
        }

        public int level() {
            return level;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SetLevelRequest {
        @JsonProperty("level")
        LoggingLevel level;

        @Override
        public String toString() {
            return "SetLevelRequest{" + "level=" + level + '}';
        }

        public LoggingLevel getLevel() {
            return level;
        }

        public void setLevel(LoggingLevel level) {
            this.level = level;
        }

        public SetLevelRequest(LoggingLevel level) {
            this.level = level;
        }
    }

    // ---------------------------
    // Autocomplete
    // ---------------------------

    public interface CompleteReference {

        String type();

        String identifier();
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CompleteRequest implements Request {
        @JsonProperty("ref")
        private final CompleteReference ref;

        @JsonProperty("argument")
        private final CompleteArgument argument;

        @JsonProperty("_meta")
        private final Map<String, Object> meta;

        @JsonProperty("context")
        private final CompleteContext context;

        @Override
        public String toString() {
            return "CompleteRequest{" + "ref="
                    + ref + ", argument="
                    + argument + ", meta="
                    + meta + ", context="
                    + context + '}';
        }

        @JsonCreator
        public CompleteRequest(
                @JsonProperty("ref") CompleteReference ref,
                @JsonProperty("argument") CompleteArgument argument,
                @JsonProperty("_meta") Map<String, Object> meta,
                @JsonProperty("context") CompleteContext context) {
            this.ref = ref;
            this.argument = argument;
            this.meta = meta;
            this.context = context;
        }

        public CompleteRequest(CompleteReference ref, CompleteArgument argument, Map<String, Object> meta) {
            this(ref, argument, meta, null);
        }

        public CompleteRequest(CompleteReference ref, CompleteArgument argument, CompleteContext context) {
            this(ref, argument, null, context);
        }

        public CompleteRequest(CompleteReference ref, CompleteArgument argument) {
            this(ref, argument, null, null);
        }

        public CompleteReference ref() {
            return ref;
        }

        public CompleteArgument argument() {
            return argument;
        }

        public Map<String, Object> meta() {
            return meta;
        }

        public CompleteContext context() {
            return context;
        }

        public static final class CompleteArgument {
            private final String name;
            private final String value;

            @JsonCreator
            public CompleteArgument(@JsonProperty("name") String name, @JsonProperty("value") String value) {
                this.name = name;
                this.value = value;
            }

            public String name() {
                return name;
            }

            public String value() {
                return value;
            }
        }

        public static final class CompleteContext {
            private final Map<String, String> arguments;

            @JsonCreator
            public CompleteContext(@JsonProperty("arguments") Map<String, String> arguments) {
                this.arguments = arguments;
            }

            public Map<String, String> arguments() {
                return arguments;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CompleteResult {
        @JsonProperty("completion")
        private final CompleteCompletion completion;

        @JsonProperty("_meta")
        private final Map<String, Object> meta;

        @Override
        public String toString() {
            return "CompleteResult{" + "completion=" + completion + ", meta=" + meta + '}';
        }

        @JsonCreator
        public CompleteResult(
                @JsonProperty("completion") CompleteCompletion completion,
                @JsonProperty("_meta") Map<String, Object> meta) {
            this.completion = completion;
            this.meta = meta;
        }

        public CompleteResult(CompleteCompletion completion) {
            this(completion, null);
        }

        public CompleteCompletion completion() {
            return completion;
        }

        public Map<String, Object> meta() {
            return meta;
        }

        public static final class CompleteCompletion {
            @JsonProperty("values")
            private final List<String> values;

            @JsonProperty("total")
            private final Integer total;

            @JsonProperty("hasMore")
            private final Boolean hasMore;

            @Override
            public String toString() {
                return "CompleteCompletion{" + "values=" + values + ", total=" + total + ", hasMore=" + hasMore + '}';
            }

            @JsonCreator
            public CompleteCompletion(
                    @JsonProperty("values") List<String> values,
                    @JsonProperty("total") Integer total,
                    @JsonProperty("hasMore") Boolean hasMore) {
                this.values = values;
                this.total = total;
                this.hasMore = hasMore;
            }

            public List<String> values() {
                return values;
            }

            public Integer total() {
                return total;
            }

            public Boolean hasMore() {
                return hasMore;
            }
        }
    }

    // ---------------------------
    // Content Types
    // ---------------------------
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TextContent.class, name = "text")
    })
    public interface Content {

        default String type() {
            if (this instanceof TextContent) {
                return "text";
            }
            throw new IllegalArgumentException("Unknown content type: " + this);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextContent implements Content { // @formatter:on
        @JsonProperty("audience")
        List<Role> audience;

        @JsonProperty("priority")
        Double priority;

        @JsonProperty("text")
        String text;

        @Override
        public String toString() {
            return "TextContent{" + "audience=" + audience + ", priority=" + priority + ", text='" + text + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TextContent that = (TextContent) o;
            return Objects.equals(audience, that.audience)
                    && Objects.equals(priority, that.priority)
                    && Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(audience, priority, text);
        }

        public List<Role> getAudience() {
            return audience;
        }

        public void setAudience(List<Role> audience) {
            this.audience = audience;
        }

        public Double getPriority() {
            return priority;
        }

        public void setPriority(Double priority) {
            this.priority = priority;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public TextContent() {}

        public TextContent(List<Role> audience, Double priority, String text) {
            this.audience = audience;
            this.priority = priority;
            this.text = text;
        }

        public TextContent(String content) {
            this(null, null, content);
        }
    }


    // ---------------------------
    // Roots
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Root {
        @JsonProperty("uri")
        String uri;

        @JsonProperty("name")
        String name;

        @Override
        public String toString() {
            return "Root{" + "uri='" + uri + '\'' + ", name='" + name + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Root root = (Root) o;
            return Objects.equals(uri, root.uri) && Objects.equals(name, root.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, name);
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Root() {}

        public Root(String uri, String name) {
            this.uri = uri;
            this.name = name;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListRootsResult {
        @JsonProperty("roots")
        List<Root> roots;

        @JsonProperty("nextCursor")
        String nextCursor;

        @Override
        public String toString() {
            return "ListRootsResult{" + "roots=" + roots + ", nextCursor='" + nextCursor + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ListRootsResult that = (ListRootsResult) o;
            return Objects.equals(roots, that.roots) && Objects.equals(nextCursor, that.nextCursor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roots, nextCursor);
        }

        public List<Root> getRoots() {
            return roots;
        }

        public void setRoots(List<Root> roots) {
            this.roots = roots;
        }

        public String getNextCursor() {
            return nextCursor;
        }

        public void setNextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
        }

        public ListRootsResult() {}

        public ListRootsResult(List<Root> roots) {
            this.roots = roots;
            this.nextCursor = null;
        }

        public ListRootsResult(List<Root> roots, String nextCursor) {
            this.roots = roots;
            this.nextCursor = nextCursor;
        }
    } // @formatter:on
}
