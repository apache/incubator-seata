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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

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

    public static final String METHOD_NOTIFICATION_PROGRESS = "notifications/progress";

    // Tool Methods
    public static final String METHOD_TOOLS_LIST = "tools/list";

    public static final String METHOD_TOOLS_CALL = "tools/call";

    public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

    // Resources Methods
    public static final String METHOD_RESOURCES_LIST = "resources/list";

    public static final String METHOD_RESOURCES_READ = "resources/read";

    public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

    public static final String METHOD_NOTIFICATION_RESOURCES_UPDATED = "notifications/resources/updated";

    public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

    public static final String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";

    public static final String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

    // Prompt Methods
    public static final String METHOD_PROMPT_LIST = "prompts/list";

    public static final String METHOD_PROMPT_GET = "prompts/get";

    public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

    public static final String METHOD_COMPLETION_COMPLETE = "completion/complete";

    // Logging Methods
    public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

    public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

    // Roots Methods
    public static final String METHOD_ROOTS_LIST = "roots/list";

    public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

    // Sampling Methods
    public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

    // Elicitation Methods
    public static final String METHOD_ELICITATION_CREATE = "elicitation/create";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ---------------------------
    // JSON-RPC Error Codes
    // ---------------------------
    /**
     * Standard error codes used in MCP JSON-RPC responses.
     */
    public static final class ErrorCodes {

        /**
         * Invalid JSON was received by the server.
         */
        public static final int PARSE_ERROR = -32700;

        /**
         * The JSON sent is not a valid Request object.
         */
        public static final int INVALID_REQUEST = -32600;

        /**
         * The method does not exist / is not available.
         */
        public static final int METHOD_NOT_FOUND = -32601;

        /**
         * Invalid method parameter(s).
         */
        public static final int INVALID_PARAMS = -32602;

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

    /**
     * Deserializes a JSON string into a JSONRPCMessage object.
     * @param objectMapper The ObjectMapper instance to use for deserialization
     * @param jsonText The JSON string to deserialize
     * @return A JSONRPCMessage instance using either the {@link JSONRPCRequest},
     * {@link JSONRPCNotification}, or {@link JSONRPCResponse} classes.
     * @throws IOException If there's an error during deserialization
     * @throws IllegalArgumentException If the JSON structure doesn't match any known
     * message type
     */
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

    /**
     * Clients can implement additional features to enrich connected MCP servers with
     * additional capabilities. These capabilities can be used to extend the functionality
     * of the server, or to provide additional information to the server about the
     * client's capabilities.
     * experimental WIP
     * roots define the boundaries of where servers can operate within the
     * filesystem, allowing them to understand which directories and files they have
     * access to.
     * sampling Provides a standardized way for servers to request LLM sampling
     * (“completions” or “generations”) from language models via clients.
     *
     */
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

        /**
         * Roots define the boundaries of where servers can operate within the filesystem,
         * allowing them to understand which directories and files they have access to.
         * Servers can request the list of roots from supporting clients and
         * receive notifications when that list changes.
         * listChanged Whether the client would send notification about roots
         * 		  has changed since the last time the server checked.
         */
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

        /**
         * Provides a standardized way for servers to request LLM
         * sampling ("completions" or "generations") from language
         * models via clients. This flow allows clients to maintain
         * control over model access, selection, and permissions
         * while enabling servers to leverage AI capabilities—with
         * no server API keys necessary. Servers can request text or
         * image-based interactions and optionally include context
         * from MCP servers in their prompts.
         */
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

    // ---------------------------
    // Resource Interfaces
    // ---------------------------
    /**
     * Base for objects that include optional annotations for the client. The client can
     * use annotations to inform how objects are used or displayed
     */
    public interface Annotated {

        Annotations getAnnotations();
    }

    /**
     * Optional annotations for the client. The client can use annotations to inform how
     * objects are used or displayed.
     * audience Describes who the intended customer of this object or data is. It
     * can include multiple entries to indicate content useful for multiple audiences
     * (e.g., `["user", "assistant"]`).
     * priority Describes how important this data is for operating the server. A
     * value of 1 means "most important," and indicates that the data is effectively
     * required, while 0 means "least important," and indicates that the data is entirely
     * optional. It is a number between 0 and 1.
     */
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

    /**
     * A common interface for resource content, which includes metadata about the resource
     * such as its URI, name, description, MIME type, size, and annotations.
     */
    public interface ResourceContent extends BaseMetadata {

        String getUri();

        String getDescription();

        String getMimeType();

        Long getSize();

        Annotations getAnnotations();
    }

    /**
     * Base interface for metadata with name (identifier) and title (display name)
     * properties.
     */
    public interface BaseMetadata {

        /**
         * Intended for programmatic or logical use, but used as a display name in past
         * specs or fallback (if title isn't present).
         */
        String getName();

        /**
         * Intended for UI and end-user contexts — optimized to be human-readable and
         * easily understood, even by those unfamiliar with domain-specific terminology.
         *
         * If not provided, the name should be used for display.
         */
        String getTitle();
    }

    /**
     * A known resource that the server is capable of reading.
     * uri the URI of the resource.
     * name A human-readable name for this resource. This can be used by clients to
     * populate UI elements.
     * description A description of what this resource represents. This can be used
     * by clients to improve the LLM's understanding of available resources. It can be
     * thought of like a "hint" to the model.
     * mimeType The MIME type of this resource, if known.
     * annotations Optional annotations for the client. The client can use
     * annotations to inform how objects are used or displayed.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource implements Annotated, ResourceContent {
        @JsonProperty("uri")
        String uri;

        @JsonProperty("name")
        String name;

        @JsonProperty("title")
        String title;

        @JsonProperty("description")
        String description;

        @JsonProperty("mimeType")
        String mimeType;

        @JsonProperty("size")
        Long size;

        @JsonProperty("annotations")
        Annotations annotations;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        public Resource() {}

        public Resource(
                String uri,
                String name,
                String title,
                String description,
                String mimeType,
                Long size,
                Annotations annotations,
                Map<String, Object> meta) {
            this.uri = uri;
            this.name = name;
            this.title = title;
            this.description = description;
            this.mimeType = mimeType;
            this.size = size;
            this.annotations = annotations;
            this.meta = meta;
        }

        @Override
        public String toString() {
            return "Resource{" + "uri='"
                    + uri + '\'' + ", name='"
                    + name + '\'' + ", title='"
                    + title + '\'' + ", description='"
                    + description + '\'' + ", mimeType='"
                    + mimeType + '\'' + ", size="
                    + size + ", annotations="
                    + annotations + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Resource resource = (Resource) o;
            return Objects.equals(uri, resource.uri)
                    && Objects.equals(name, resource.name)
                    && Objects.equals(title, resource.title)
                    && Objects.equals(description, resource.description)
                    && Objects.equals(mimeType, resource.mimeType)
                    && Objects.equals(size, resource.size)
                    && Objects.equals(annotations, resource.annotations)
                    && Objects.equals(meta, resource.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, name, title, description, mimeType, size, annotations, meta);
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

        public void setTitle(String title) {
            this.title = title;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        @Override
        public String getTitle() {
            return this.title;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getMimeType() {
            return mimeType;
        }

        @Override
        public Long getSize() {
            return this.size;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public Annotations getAnnotations() {
            return annotations;
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations = annotations;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private String uri;

            private String name;

            private String title;

            private String description;

            private String mimeType;

            private Long size;

            private Annotations annotations;

            private Map<String, Object> meta;

            public Builder uri(String uri) {
                this.uri = uri;
                return this;
            }

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

            public Builder mimeType(String mimeType) {
                this.mimeType = mimeType;
                return this;
            }

            public Builder size(Long size) {
                this.size = size;
                return this;
            }

            public Builder annotations(Annotations annotations) {
                this.annotations = annotations;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public Resource build() {
                Assert.hasText(uri, "uri must not be empty");
                Assert.hasText(name, "name must not be empty");

                return new Resource(uri, name, title, description, mimeType, size, annotations, meta);
            }
        }
    } // @formatter:on

    /**
     * Resource templates allow servers to expose parameterized resources using URI
     * templates.
     * uriTemplate A URI template that can be used to generate URIs for this
     * resource.
     * name A human-readable name for this resource. This can be used by clients to
     * populate UI elements.
     * description A description of what this resource represents. This can be used
     * by clients to improve the LLM's understanding of available resources. It can be
     * thought of like a "hint" to the model.
     * mimeType The MIME type of this resource, if known.
     * annotations Optional annotations for the client. The client can use
     * annotations to inform how objects are used or displayed.
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6570">RFC 6570</a>
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceTemplate implements Annotated, BaseMetadata {
        @JsonProperty("uriTemplate")
        String uriTemplate;

        @JsonProperty("name")
        String name;

        @JsonProperty("title")
        String title;

        @JsonProperty("description")
        String description;

        @JsonProperty("mimeType")
        String mimeType;

        @JsonProperty("annotations")
        Annotations annotations;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ResourceTemplate{" + "uriTemplate='"
                    + uriTemplate + '\'' + ", name='"
                    + name + '\'' + ", title='"
                    + title + '\'' + ", description='"
                    + description + '\'' + ", mimeType='"
                    + mimeType + '\'' + ", annotations="
                    + annotations + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ResourceTemplate that = (ResourceTemplate) o;
            return Objects.equals(uriTemplate, that.uriTemplate)
                    && Objects.equals(name, that.name)
                    && Objects.equals(title, that.title)
                    && Objects.equals(description, that.description)
                    && Objects.equals(mimeType, that.mimeType)
                    && Objects.equals(annotations, that.annotations)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uriTemplate, name, title, description, mimeType, annotations, meta);
        }

        public String getUriTemplate() {
            return uriTemplate;
        }

        public void setUriTemplate(String uriTemplate) {
            this.uriTemplate = uriTemplate;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        @Override
        public Annotations getAnnotations() {
            return annotations;
        }

        public void setAnnotations(Annotations annotations) {
            this.annotations = annotations;
        }

        public ResourceTemplate() {}

        public ResourceTemplate(
                String uriTemplate,
                String name,
                String title,
                String description,
                String mimeType,
                Annotations annotations,
                Map<String, Object> meta) {
            this.uriTemplate = uriTemplate;
            this.name = name;
            this.title = title;
            this.description = description;
            this.mimeType = mimeType;
            this.annotations = annotations;
            this.meta = meta;
        }

        public ResourceTemplate(
                String uriTemplate,
                String name,
                String title,
                String description,
                String mimeType,
                Annotations annotations) {
            this(uriTemplate, name, title, description, mimeType, annotations, null);
        }

        public ResourceTemplate(
                String uriTemplate, String name, String description, String mimeType, Annotations annotations) {
            this(uriTemplate, name, null, description, mimeType, annotations);
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResourcesResult implements Result {
        @JsonProperty("resources")
        List<Resource> resources;

        @JsonProperty("nextCursor")
        String nextCursor;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ListResourcesResult{" + "resources="
                    + resources + ", nextCursor='"
                    + nextCursor + '\'' + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ListResourcesResult that = (ListResourcesResult) o;
            return Objects.equals(resources, that.resources)
                    && Objects.equals(nextCursor, that.nextCursor)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resources, nextCursor, meta);
        }

        public List<Resource> getResources() {
            return resources;
        }

        public void setResources(List<Resource> resources) {
            this.resources = resources;
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

        public ListResourcesResult() {}

        public ListResourcesResult(List<Resource> resources, String nextCursor) {
            this.resources = resources;
            this.nextCursor = nextCursor;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResourceTemplatesResult implements Result {
        @JsonProperty("resourceTemplates")
        List<ResourceTemplate> resourceTemplates;

        @JsonProperty("nextCursor")
        String nextCursor;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ListResourceTemplatesResult{" + "resourceTemplates="
                    + resourceTemplates + ", nextCursor='"
                    + nextCursor + '\'' + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ListResourceTemplatesResult that = (ListResourceTemplatesResult) o;
            return Objects.equals(resourceTemplates, that.resourceTemplates)
                    && Objects.equals(nextCursor, that.nextCursor)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceTemplates, nextCursor, meta);
        }

        public List<ResourceTemplate> getResourceTemplates() {
            return resourceTemplates;
        }

        public void setResourceTemplates(List<ResourceTemplate> resourceTemplates) {
            this.resourceTemplates = resourceTemplates;
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

        public ListResourceTemplatesResult() {}

        public ListResourceTemplatesResult(String nextCursor, List<ResourceTemplate> resourceTemplates) {
            this.nextCursor = nextCursor;
            this.resourceTemplates = resourceTemplates;
            this.meta = null;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReadResourceRequest implements Request {
        @JsonProperty("uri")
        String uri;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ReadResourceRequest{" + "uri='" + uri + '\'' + ", meta=" + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ReadResourceRequest that = (ReadResourceRequest) o;
            return Objects.equals(uri, that.uri) && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, meta);
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public ReadResourceRequest() {}

        public ReadResourceRequest(String uri) {
            this.uri = uri;
            this.meta = null;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReadResourceResult implements Result {
        @JsonProperty("contents")
        List<ResourceContents> contents;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ReadResourceResult{" + "contents=" + contents + ", meta=" + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ReadResourceResult that = (ReadResourceResult) o;
            return Objects.equals(contents, that.contents) && Objects.equals(meta, that.meta);
        }

        public int hashCode() {
            return Objects.hashCode(contents);
        }

        public List<ResourceContents> getContents() {
            return contents;
        }

        public void setContents(List<ResourceContents> contents) {
            this.contents = contents;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public ReadResourceResult() {}

        public ReadResourceResult(List<ResourceContents> contents) {
            this.contents = contents;
            this.meta = null;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    /**
     * Sent from the client to request resources/updated notifications from the server
     * whenever a particular resource changes.
     * uri the URI of the resource to subscribe to. The URI can use any protocol;
     * it is up to the server how to interpret it.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscribeRequest implements Request {
        @JsonProperty("uri")
        String uri;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "SubscribeRequest{" + "uri='" + uri + '\'' + ", meta=" + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SubscribeRequest that = (SubscribeRequest) o;
            return Objects.equals(uri, that.uri) && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, meta);
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public SubscribeRequest() {}

        public SubscribeRequest(String uri) {
            this.uri = uri;
            this.meta = null;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnsubscribeRequest implements Request {
        @JsonProperty("uri")
        String uri;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "UnsubscribeRequest{" + "uri='" + uri + '\'' + ", meta=" + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            UnsubscribeRequest that = (UnsubscribeRequest) o;
            return Objects.equals(uri, that.uri) && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, meta);
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public UnsubscribeRequest() {}

        public UnsubscribeRequest(String uri) {
            this.uri = uri;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    /**
     * The contents of a specific resource or sub-resource.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, include = As.PROPERTY)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TextResourceContents.class, name = "text"),
        @JsonSubTypes.Type(value = BlobResourceContents.class, name = "blob")
    })
    public interface ResourceContents {

        /**
         * The URI of this resource.
         * @return the URI of this resource.
         */
        String getUri();

        /**
         * The MIME type of this resource.
         * @return the MIME type of this resource.
         */
        String getMimeType();

        /**
         * @see <a href=
         * "https://modelcontextprotocol.io/specification/2025-06-18/basic/index#meta">Specification</a>
         * for notes on _meta usage
         * @return additional metadata related to this resource.
         */
        Map<String, Object> meta();
    }

    /**
     * Text contents of a resource.
     * uri the URI of this resource.
     * mimeType the MIME type of this resource.
     * text the text of the resource. This must only be set if the resource can
     * actually be represented as text (not binary data).
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextResourceContents implements ResourceContents {
        @JsonProperty("uri")
        String uri;

        @JsonProperty("mimeType")
        String mimeType;

        @JsonProperty("text")
        String text;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "TextResourceContents{" + "uri='"
                    + uri + '\'' + ", mimeType='"
                    + mimeType + '\'' + ", text='"
                    + text + '\'' + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TextResourceContents that = (TextResourceContents) o;
            return Objects.equals(uri, that.uri)
                    && Objects.equals(mimeType, that.mimeType)
                    && Objects.equals(text, that.text)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, mimeType, text, meta);
        }

        @Override
        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public TextResourceContents() {}

        public TextResourceContents(String text, String mimeType, String uri) {
            this.text = text;
            this.mimeType = mimeType;
            this.uri = uri;
            this.meta = null;
        }
    } // @formatter:on

    /**
     * Binary contents of a resource.
     * uri the URI of this resource.
     * mimeType the MIME type of this resource.
     * blob a base64-encoded string representing the binary data of the resource.
     * This must only be set if the resource can actually be represented as binary data
     * (not text).
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BlobResourceContents implements ResourceContents {
        @JsonProperty("uri")
        String uri;

        @JsonProperty("mimeType")
        String mimeType;

        @JsonProperty("blob")
        String blob;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "BlobResourceContents{" + "uri='"
                    + uri + '\'' + ", mimeType='"
                    + mimeType + '\'' + ", blob='"
                    + blob + '\'' + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            BlobResourceContents that = (BlobResourceContents) o;
            return Objects.equals(uri, that.uri)
                    && Objects.equals(mimeType, that.mimeType)
                    && Objects.equals(blob, that.blob)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, mimeType, blob, meta);
        }

        @Override
        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getBlob() {
            return blob;
        }

        public void setBlob(String blob) {
            this.blob = blob;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public BlobResourceContents() {}

        public BlobResourceContents(String uri, String mimeType, String blob) {
            this.uri = uri;
            this.mimeType = mimeType;
            this.blob = blob;
            this.meta = null;
        }
    } // @formatter:on

    // ---------------------------
    // Prompt Interfaces
    // ---------------------------
    /**
     * A prompt or prompt template that the server offers.
     * name The name of the prompt or prompt template.
     * description An optional description of what this prompt provides.
     * arguments A list of arguments to use for templating the prompt.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Prompt implements BaseMetadata {
        @JsonProperty("name")
        String name;

        @JsonProperty("title")
        String title;

        @JsonProperty("description")
        String description;

        @JsonProperty("arguments")
        List<PromptArgument> arguments;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "Prompt{" + "name='"
                    + name + '\'' + ", title='"
                    + title + '\'' + ", description='"
                    + description + '\'' + ", arguments="
                    + arguments + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Prompt prompt = (Prompt) o;
            return Objects.equals(name, prompt.name)
                    && Objects.equals(title, prompt.title)
                    && Objects.equals(description, prompt.description)
                    && Objects.equals(arguments, prompt.arguments)
                    && Objects.equals(meta, prompt.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, title, description, arguments, meta);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<PromptArgument> getArguments() {
            return arguments;
        }

        public void setArguments(List<PromptArgument> arguments) {
            this.arguments = arguments;
        }

        @Override
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public Prompt() {}

        public Prompt(
                String name,
                String title,
                String description,
                List<PromptArgument> arguments,
                Map<String, Object> meta) {
            this.name = name;
            this.title = title;
            this.description = description;
            this.arguments = arguments;
            this.meta = meta;
        }

        public Prompt(String name, String description, List<PromptArgument> arguments) {
            this(name, null, description, arguments != null ? arguments : new ArrayList<>());
        }

        public Prompt(String name, String title, String description, List<PromptArgument> arguments) {
            this(name, title, description, arguments != null ? arguments : new ArrayList<>(), null);
        }
    } // @formatter:on

    /**
     * Describes an argument that a prompt can accept.
     * name The name of the argument.
     * description A human-readable description of the argument.
     * required Whether this argument must be provided.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptArgument implements BaseMetadata {
        @JsonProperty("name")
        String name;

        @JsonProperty("title")
        String title;

        @JsonProperty("description")
        String description;

        @JsonProperty("required")
        Boolean required;

        @Override
        public String toString() {
            return "PromptArgument{" + "name='"
                    + name + '\'' + ", title='"
                    + title + '\'' + ", description='"
                    + description + '\'' + ", required="
                    + required + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PromptArgument that = (PromptArgument) o;
            return Objects.equals(name, that.name)
                    && Objects.equals(title, that.title)
                    && Objects.equals(description, that.description)
                    && Objects.equals(required, that.required);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, title, description, required);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        @Override
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public PromptArgument() {}

        public PromptArgument(String name, String description, Boolean required) {
            this.name = name;
            this.title = null;
            this.description = description;
            this.required = required;
        }
    } // @formatter:on

    /**
     * Describes a message returned as part of a prompt.
     * This is similar to `SamplingMessage`, but also supports the embedding of resources
     * from the MCP server.
     * role The sender or recipient of messages and data in a conversation.
     * content The content of the message of type {@link Content}.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptMessage {
        @JsonProperty("role")
        Role role;

        @JsonProperty("content")
        Content content;

        @Override
        public String toString() {
            return "PromptMessage{" + "role=" + role + ", content=" + content + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PromptMessage that = (PromptMessage) o;
            return role == that.role && Objects.equals(content, that.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(role, content);
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public PromptMessage() {}

        public PromptMessage(Role role, Content content) {
            this.role = role;
            this.content = content;
        }
    } // @formatter:on

    /**
     * The server's response to a prompts/list request from the client.
     * prompts A list of prompts that the server provides.
     * nextCursor An optional cursor for pagination. If present, indicates there
     * are more prompts available.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListPromptsResult implements Result {
        @JsonProperty("prompts")
        List<Prompt> prompts;

        @JsonProperty("nextCursor")
        String nextCursor;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ListPromptsResult{" + "prompts="
                    + prompts + ", nextCursor='"
                    + nextCursor + '\'' + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ListPromptsResult that = (ListPromptsResult) o;
            return Objects.equals(prompts, that.prompts)
                    && Objects.equals(nextCursor, that.nextCursor)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prompts, nextCursor, meta);
        }

        public List<Prompt> getPrompts() {
            return prompts;
        }

        public void setPrompts(List<Prompt> prompts) {
            this.prompts = prompts;
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

        public ListPromptsResult() {}

        public ListPromptsResult(List<Prompt> prompts, String nextCursor) {
            this.prompts = prompts;
            this.nextCursor = nextCursor;
            this.meta = null;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    /**
     * Used by the client to get a prompt provided by the server.
     * name The name of the prompt or prompt template.
     * Arguments to use for templating the prompt.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetPromptRequest implements Request {
        @JsonProperty("name")
        String name;

        @JsonProperty("arguments")
        Map<String, Object> arguments;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "GetPromptRequest{" + "name='" + name + '\'' + ", arguments=" + arguments + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            GetPromptRequest that = (GetPromptRequest) o;
            return Objects.equals(name, that.name) && Objects.equals(arguments, that.arguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arguments);
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

        public GetPromptRequest() {}

        public GetPromptRequest(String name, Map<String, Object> arguments) {
            this.name = name;
            this.arguments = arguments;
            this.meta = null;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:off

    /**
     * The server's response to a prompts/get request from the client.
     * description An optional description for the prompt.
     * messages A list of messages to display as part of the prompt.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetPromptResult implements Result {
        @JsonProperty("description")
        String description;

        @JsonProperty("messages")
        List<PromptMessage> messages;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        public GetPromptResult() {}

        public GetPromptResult(String description, List<PromptMessage> messages) {
            this.description = description;
            this.messages = messages;
            this.meta = null;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<PromptMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<PromptMessage> messages) {
            this.messages = messages;
        }

        @Override
        public String toString() {
            return "GetPromptResult{" + "description='" + description + '\'' + ", messages=" + messages + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            GetPromptResult that = (GetPromptResult) o;
            return Objects.equals(description, that.description) && Objects.equals(messages, that.messages);
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, messages);
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    // ---------------------------
    // Tool Interfaces
    // ---------------------------
    /**
     * The server's response to a tools/list request from the client.
     * tools A list of tools that the server provides.
     * nextCursor An optional cursor for pagination. If present, indicates there
     * are more tools available.
     */
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

    /**
     * Additional properties describing a Tool to clients.
     * NOTE: all properties in ToolAnnotations are **hints**. They are not guaranteed to
     * provide a faithful description of tool behavior (including descriptive properties
     * like `title`).
     * Clients should never make tool use decisions based on ToolAnnotations received from
     * untrusted servers.
     */
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

    /**
     * Represents a tool that the server provides. Tools enable servers to expose
     * executable functionality to the system. Through these tools, you can interact with
     * external systems, perform computations, and take actions in the real world.
     * name A unique identifier for the tool. This name is used when calling the
     * tool.
     * description A human-readable description of what the tool does. This can be
     * used by clients to improve the LLM's understanding of available tools.
     * inputSchema A JSON Schema object that describes the expected structure of
     * the arguments when calling this tool. This allows clients to validate tool
     * arguments before sending them to the server.
     */
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

    /**
     * Used by the client to call a tool provided by the server.
     * name The name of the tool to call. This must match a tool name from
     * tools/list.
     * Arguments to pass to the tool. These must conform to the tool's
     * input schema.
     */
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

    /**
     * The server's response to a tools/call request from the client.
     * content A list of content items representing the tool's output. Each item can be text, an image,
     *                or an embedded resource.
     * isError If true, indicates that the tool execution failed and the content contains error information.
     *                If false or absent, indicates successful execution.
     */
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

        /**
         * Creates a new instance of {@link CallToolResult} with a string containing the
         * tool result.
         * @param content The content of the tool result. This will be mapped to a
         * one-sized list with a {@link TextContent} element.
         * @param isError If true, indicates that the tool execution failed and the
         * content contains error information. If false or absent, indicates successful
         * execution.
         */
        public CallToolResult(String content, Boolean isError) {
            this(Collections.singletonList(new TextContent(content)), isError, null);
        }

        /**
         * Creates a builder for {@link CallToolResult}.
         * @return a new builder instance
         */
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

            /**
             * Sets the content list for the tool result.
             * @param content the content list
             * @return this builder
             */
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

            /**
             * Sets the text content for the tool result.
             * @param textContent the text content
             * @return this builder
             */
            public Builder textContent(List<String> textContent) {
                Assert.notNull(textContent, "textContent must not be null");
                textContent.stream().map(TextContent::new).forEach(this.content::add);
                return this;
            }

            /**
             * Adds a content item to the tool result.
             * @param contentItem the content item to add
             * @return this builder
             */
            public Builder addContent(Content contentItem) {
                Assert.notNull(contentItem, "contentItem must not be null");
                if (this.content == null) {
                    this.content = new ArrayList<>();
                }
                this.content.add(contentItem);
                return this;
            }

            /**
             * Adds a text content item to the tool result.
             * @param text the text content
             * @return this builder
             */
            public Builder addTextContent(String text) {
                Assert.notNull(text, "text must not be null");
                return addContent(new TextContent(text));
            }

            /**
             * Sets whether the tool execution resulted in an error.
             * @param isError true if the tool execution failed, false otherwise
             * @return this builder
             */
            public Builder isError(Boolean isError) {
                Assert.notNull(isError, "isError must not be null");
                this.isError = isError;
                return this;
            }

            /**
             * Sets the metadata for the tool result.
             * @param meta metadata
             * @return this builder
             */
            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            /**
             * Builds a new {@link CallToolResult} instance.
             * @return a new CallToolResult instance
             */
            public CallToolResult build() {
                return new CallToolResult(content, isError, structuredContent, meta);
            }
        }
    } // @formatter:on

    // ---------------------------
    // Sampling Interfaces
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelPreferences {
        @JsonProperty("hints")
        List<ModelHint> hints;

        @JsonProperty("costPriority")
        Double costPriority;

        @JsonProperty("speedPriority")
        Double speedPriority;

        @JsonProperty("intelligencePriority")
        Double intelligencePriority;

        @Override
        public String toString() {
            return "ModelPreferences{" + "hints="
                    + hints + ", costPriority="
                    + costPriority + ", speedPriority="
                    + speedPriority + ", intelligencePriority="
                    + intelligencePriority + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ModelPreferences that = (ModelPreferences) o;
            return Objects.equals(hints, that.hints)
                    && Objects.equals(costPriority, that.costPriority)
                    && Objects.equals(speedPriority, that.speedPriority)
                    && Objects.equals(intelligencePriority, that.intelligencePriority);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hints, costPriority, speedPriority, intelligencePriority);
        }

        public List<ModelHint> getHints() {
            return hints;
        }

        public void setHints(List<ModelHint> hints) {
            this.hints = hints;
        }

        public Double getCostPriority() {
            return costPriority;
        }

        public void setCostPriority(Double costPriority) {
            this.costPriority = costPriority;
        }

        public Double getSpeedPriority() {
            return speedPriority;
        }

        public void setSpeedPriority(Double speedPriority) {
            this.speedPriority = speedPriority;
        }

        public Double getIntelligencePriority() {
            return intelligencePriority;
        }

        public void setIntelligencePriority(Double intelligencePriority) {
            this.intelligencePriority = intelligencePriority;
        }

        public ModelPreferences() {}

        public ModelPreferences(
                List<ModelHint> hints, Double costPriority, Double speedPriority, Double intelligencePriority) {
            this.hints = hints;
            this.costPriority = costPriority;
            this.speedPriority = speedPriority;
            this.intelligencePriority = intelligencePriority;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<ModelHint> hints;
            private Double costPriority;
            private Double speedPriority;
            private Double intelligencePriority;

            public Builder hints(List<ModelHint> hints) {
                this.hints = hints;
                return this;
            }

            public Builder addHint(String name) {
                if (this.hints == null) {
                    this.hints = new ArrayList<>();
                }
                this.hints.add(new ModelHint(name));
                return this;
            }

            public Builder costPriority(Double costPriority) {
                this.costPriority = costPriority;
                return this;
            }

            public Builder speedPriority(Double speedPriority) {
                this.speedPriority = speedPriority;
                return this;
            }

            public Builder intelligencePriority(Double intelligencePriority) {
                this.intelligencePriority = intelligencePriority;
                return this;
            }

            public ModelPreferences build() {
                return new ModelPreferences(hints, costPriority, speedPriority, intelligencePriority);
            }
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelHint {
        @JsonProperty("name")
        String name;

        @Override
        public String toString() {
            return "ModelHint{" + "name='" + name + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ModelHint modelHint = (ModelHint) o;
            return Objects.equals(name, modelHint.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ModelHint() {}

        public ModelHint(String name) {
            this.name = name;
        }

        public static ModelHint of(String name) {
            return new ModelHint(name);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SamplingMessage {
        @JsonProperty("role")
        Role role;

        @JsonProperty("content")
        Content content;

        @Override
        public String toString() {
            return "SamplingMessage{" + "role=" + role + ", content=" + content + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SamplingMessage that = (SamplingMessage) o;
            return role == that.role && Objects.equals(content, that.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(role, content);
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public SamplingMessage() {}

        public SamplingMessage(Role role, Content content) {
            this.role = role;
            this.content = content;
        }
    } // @formatter:on

    // Sampling and Message Creation
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateMessageRequest implements Request {
        @JsonProperty("messages")
        List<SamplingMessage> messages;

        @JsonProperty("modelPreferences")
        ModelPreferences modelPreferences;

        @JsonProperty("systemPrompt")
        String systemPrompt;

        @JsonProperty("includeContext")
        ContextInclusionStrategy includeContext;

        @JsonProperty("temperature")
        Double temperature;

        @JsonProperty("maxTokens")
        int maxTokens;

        @JsonProperty("stopSequences")
        List<String> stopSequences;

        @JsonProperty("metadata")
        Map<String, Object> metadata;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "CreateMessageRequest{" + "messages="
                    + messages + ", modelPreferences="
                    + modelPreferences + ", systemPrompt='"
                    + systemPrompt + '\'' + ", includeContext="
                    + includeContext + ", temperature="
                    + temperature + ", maxTokens="
                    + maxTokens + ", stopSequences="
                    + stopSequences + ", metadata="
                    + metadata + ", meta="
                    + meta + '}';
        }

        public CreateMessageRequest(
                List<SamplingMessage> messages,
                ModelPreferences modelPreferences,
                String systemPrompt,
                ContextInclusionStrategy includeContext,
                Double temperature,
                int maxTokens,
                List<String> stopSequences,
                Map<String, Object> metadata,
                Map<String, Object> meta) {
            this.messages = messages;
            this.modelPreferences = modelPreferences;
            this.systemPrompt = systemPrompt;
            this.includeContext = includeContext;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
            this.stopSequences = stopSequences;
            this.metadata = metadata;
            this.meta = meta;
        }

        // backwards compatibility constructor
        public CreateMessageRequest(
                List<SamplingMessage> messages,
                ModelPreferences modelPreferences,
                String systemPrompt,
                ContextInclusionStrategy includeContext,
                Double temperature,
                int maxTokens,
                List<String> stopSequences,
                Map<String, Object> metadata) {
            this(
                    messages,
                    modelPreferences,
                    systemPrompt,
                    includeContext,
                    temperature,
                    maxTokens,
                    stopSequences,
                    metadata,
                    null);
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        public List<SamplingMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<SamplingMessage> messages) {
            this.messages = messages;
        }

        public ModelPreferences getModelPreferences() {
            return modelPreferences;
        }

        public void setModelPreferences(ModelPreferences modelPreferences) {
            this.modelPreferences = modelPreferences;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public ContextInclusionStrategy getIncludeContext() {
            return includeContext;
        }

        public void setIncludeContext(ContextInclusionStrategy includeContext) {
            this.includeContext = includeContext;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public List<String> getStopSequences() {
            return stopSequences;
        }

        public void setStopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public enum ContextInclusionStrategy {

            // @formatter:off
            @JsonProperty("none")
            NONE,
            @JsonProperty("thisServer")
            THIS_SERVER,
            @JsonProperty("allServers")
            ALL_SERVERS
        } // @formatter:on

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private List<SamplingMessage> messages;

            private ModelPreferences modelPreferences;

            private String systemPrompt;

            private ContextInclusionStrategy includeContext;

            private Double temperature;

            private int maxTokens;

            private List<String> stopSequences;

            private Map<String, Object> metadata;

            private Map<String, Object> meta;

            public Builder messages(List<SamplingMessage> messages) {
                this.messages = messages;
                return this;
            }

            public Builder modelPreferences(ModelPreferences modelPreferences) {
                this.modelPreferences = modelPreferences;
                return this;
            }

            public Builder systemPrompt(String systemPrompt) {
                this.systemPrompt = systemPrompt;
                return this;
            }

            public Builder includeContext(ContextInclusionStrategy includeContext) {
                this.includeContext = includeContext;
                return this;
            }

            public Builder temperature(Double temperature) {
                this.temperature = temperature;
                return this;
            }

            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }

            public Builder stopSequences(List<String> stopSequences) {
                this.stopSequences = stopSequences;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
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

            public CreateMessageRequest build() {
                return new CreateMessageRequest(
                        messages,
                        modelPreferences,
                        systemPrompt,
                        includeContext,
                        temperature,
                        maxTokens,
                        stopSequences,
                        metadata,
                        meta);
            }
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateMessageResult implements Result {
        @JsonProperty("role")
        Role role;

        @JsonProperty("content")
        Content content;

        @JsonProperty("model")
        String model;

        @JsonProperty("stopReason")
        StopReason stopReason;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "CreateMessageResult{" + "role="
                    + role + ", content="
                    + content + ", model='"
                    + model + '\'' + ", stopReason="
                    + stopReason + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            CreateMessageResult that = (CreateMessageResult) o;
            return role == that.role
                    && Objects.equals(content, that.content)
                    && Objects.equals(model, that.model)
                    && stopReason == that.stopReason
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(role, content, model, stopReason, meta);
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public StopReason getStopReason() {
            return stopReason;
        }

        public void setStopReason(StopReason stopReason) {
            this.stopReason = stopReason;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public CreateMessageResult() {}

        public CreateMessageResult(
                Role role, Content content, String model, StopReason stopReason, Map<String, Object> meta) {
            this.role = role;
            this.content = content;
            this.model = model;
            this.stopReason = stopReason;
            this.meta = meta;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        public enum StopReason {
            @JsonProperty("endTurn")
            END_TURN("endTurn"),

            @JsonProperty("stopSequence")
            STOP_SEQUENCE("stopSequence"),

            @JsonProperty("maxTokens")
            MAX_TOKENS("maxTokens"),

            @JsonProperty("unknown")
            UNKNOWN("unknown");

            private final String value;

            private StopReason(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }

            @JsonCreator
            public static StopReason fromValue(String value) {
                for (StopReason reason : values()) {
                    if (reason.value.equals(value)) {
                        return reason;
                    }
                }
                return UNKNOWN;
            }
        }

        public CreateMessageResult(Role role, Content content, String model, StopReason stopReason) {
            this(role, content, model, stopReason, null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private Role role = Role.ASSISTANT;

            private Content content;

            private String model;

            private StopReason stopReason = StopReason.END_TURN;

            private Map<String, Object> meta;

            public Builder role(Role role) {
                this.role = role;
                return this;
            }

            public Builder content(Content content) {
                this.content = content;
                return this;
            }

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder stopReason(StopReason stopReason) {
                this.stopReason = stopReason;
                return this;
            }

            public Builder message(String message) {
                this.content = new TextContent(message);
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public CreateMessageResult build() {
                return new CreateMessageResult(role, content, model, stopReason, meta);
            }
        }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ElicitRequest implements Request { // @formatter:on

        @JsonProperty("message")
        String message;

        @JsonProperty("requestedSchema")
        Map<String, Object> requestedSchema;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ElicitRequest{" + "message='"
                    + message + '\'' + ", requestedSchema="
                    + requestedSchema + ", meta="
                    + meta + '}';
        }

        // backwards compatibility constructor
        public ElicitRequest(String message, Map<String, Object> requestedSchema, Map<String, Object> meta) {
            this.message = message;
            this.requestedSchema = requestedSchema;
            this.meta = meta;
        }

        public ElicitRequest(String message, Map<String, Object> requestedSchema) {
            this(message, requestedSchema, null);
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private String message;

            private Map<String, Object> requestedSchema;

            private Map<String, Object> meta;

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder requestedSchema(Map<String, Object> requestedSchema) {
                this.requestedSchema = requestedSchema;
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

            public ElicitRequest build() {
                return new ElicitRequest(message, requestedSchema, meta);
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ElicitResult implements Result { // @formatter:on

        @JsonProperty("action")
        Action action;

        @JsonProperty("content")
        Map<String, Object> content;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ElicitResult{" + "action=" + action + ", content=" + content + ", meta=" + meta + '}';
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        public enum Action {

            // @formatter:off
            @JsonProperty("accept")
            ACCEPT,
            @JsonProperty("decline")
            DECLINE,
            @JsonProperty("cancel")
            CANCEL
        } // @formatter:on

        // backwards compatibility constructor
        public ElicitResult(Action action, Map<String, Object> content, Map<String, Object> meta) {
            this.action = action;
            this.content = content;
            this.meta = meta;
        }

        public ElicitResult(Action action, Map<String, Object> content) {
            this(action, content, null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private Action action;

            private Map<String, Object> content;

            private Map<String, Object> meta;

            public Builder message(Action action) {
                this.action = action;
                return this;
            }

            public Builder content(Map<String, Object> content) {
                this.content = content;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public ElicitResult build() {
                return new ElicitResult(action, content, meta);
            }
        }
    }

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
    // Progress and Logging
    // ---------------------------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgressNotification implements Notification {
        @JsonProperty("progressToken")
        String progressToken;

        @JsonProperty("progress")
        double progress;

        @JsonProperty("total")
        Double total;

        @JsonProperty("message")
        String message;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        public ProgressNotification(
                String progressToken, double progress, Double total, String message, Map<String, Object> meta) {
            this.progressToken = progressToken;
            this.progress = progress;
            this.total = total;
            this.message = message;
            this.meta = meta;
        }

        @Override
        public String toString() {
            return "ProgressNotification{" + "progressToken='"
                    + progressToken + '\'' + ", progress="
                    + progress + ", total="
                    + total + ", message='"
                    + message + '\'' + ", meta="
                    + meta + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ProgressNotification that = (ProgressNotification) o;
            return Double.compare(progress, that.progress) == 0
                    && Objects.equals(progressToken, that.progressToken)
                    && Objects.equals(total, that.total)
                    && Objects.equals(message, that.message)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(progressToken, progress, total, message, meta);
        }

        public String getProgressToken() {
            return progressToken;
        }

        public void setProgressToken(String progressToken) {
            this.progressToken = progressToken;
        }

        public double getProgress() {
            return progress;
        }

        public void setProgress(double progress) {
            this.progress = progress;
        }

        public Double getTotal() {
            return total;
        }

        public void setTotal(Double total) {
            this.total = total;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        public ProgressNotification(String progressToken, double progress, Double total, String message) {
            this(progressToken, progress, total, message, null);
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    } // @formatter:on

    /**
     * The Model Context Protocol (MCP) provides a standardized way for servers to send
     * resources update message to clients.
     *
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourcesUpdatedNotification implements Notification {

        @JsonProperty("uri")
        String uri;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        @Override
        public String toString() {
            return "ResourcesUpdatedNotification{" + "uri='" + uri + '\'' + ", meta=" + meta + '}';
        }

        public ResourcesUpdatedNotification(String uri, Map<String, Object> meta) {
            this.uri = uri;
            this.meta = meta;
        }

        public ResourcesUpdatedNotification(String uri) {
            this(uri, null);
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }
    }

    /**
     * The Model Context Protocol (MCP) provides a standardized way for servers to send
     * structured log messages to clients. Clients can control logging verbosity by
     * setting minimum log levels, with servers sending notifications containing severity
     * levels, optional logger names, and arbitrary JSON-serializable data.
     * level The severity levels. The mimimum log level is set by the client.
     * logger The logger that generated the message.
     * data JSON-serializable logging data.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoggingMessageNotification implements Notification {
        @JsonProperty("level")
        LoggingLevel level;

        @JsonProperty("logger")
        String logger;

        @JsonProperty("data")
        String data;

        @JsonProperty("_meta")
        Map<String, Object> meta;

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            LoggingMessageNotification that = (LoggingMessageNotification) o;
            return level == that.level
                    && Objects.equals(logger, that.logger)
                    && Objects.equals(data, that.data)
                    && Objects.equals(meta, that.meta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, logger, data, meta);
        }

        @Override
        public String toString() {
            return "LoggingMessageNotification{" + "level="
                    + level + ", logger='"
                    + logger + '\'' + ", data='"
                    + data + '\'' + ", meta="
                    + meta + '}';
        }

        public LoggingLevel getLevel() {
            return level;
        }

        public void setLevel(LoggingLevel level) {
            this.level = level;
        }

        public String getLogger() {
            return logger;
        }

        public void setLogger(String logger) {
            this.logger = logger;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public LoggingMessageNotification() {}

        public LoggingMessageNotification(LoggingLevel level, String logger, String data, Map<String, Object> meta) {
            this.level = level;
            this.logger = logger;
            this.data = data;
            this.meta = meta;
        }

        @Override
        public Map<String, Object> meta() {
            return this.meta;
        }

        public LoggingMessageNotification(LoggingLevel level, String logger, String data) {
            this(level, logger, data, null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private LoggingLevel level = LoggingLevel.INFO;

            private String logger = "server";

            private String data;

            private Map<String, Object> meta;

            public Builder level(LoggingLevel level) {
                this.level = level;
                return this;
            }

            public Builder logger(String logger) {
                this.logger = logger;
                return this;
            }

            public Builder data(String data) {
                this.data = data;
                return this;
            }

            public Builder meta(Map<String, Object> meta) {
                this.meta = meta;
                return this;
            }

            public LoggingMessageNotification build() {
                return new LoggingMessageNotification(level, logger, data, meta);
            }
        }
    } // @formatter:on

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

    /**
     * A request from the client to the server, to enable or adjust logging.
     * level The level of logging that the client wants to receive from the server.
     * The server should send all logs at this level and higher (i.e., more severe) to the
     * client as notifications/message
     */
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

    /**
     * Identifies a prompt for completion requests.
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class PromptReference implements CompleteReference, BaseMetadata { // @formatter:on

        @JsonProperty("type")
        String type;

        @JsonProperty("name")
        String name;

        @JsonProperty("title")
        String title;

        @Override
        public String toString() {
            return "PromptReference{" + "type='"
                    + type + '\'' + ", name='"
                    + name + '\'' + ", title='"
                    + title + '\'' + '}';
        }

        public PromptReference(String type, String name) {
            this.type = type;
            this.name = name;
            this.title = null;
        }

        public PromptReference(String name) {
            this.name = name;
            this.type = "ref/prompt";
            this.title = null;
        }

        public PromptReference(String type, String name, String title) {
            this.type = type;
            this.name = name;
            this.title = title;
        }

        @Override
        public String type() {
            return getType();
        }

        @Override
        public String identifier() {
            return getName();
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * A reference to a resource or resource template definition for completion requests.
     *
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ResourceReference implements CompleteReference {

        @JsonProperty("type")
        String type;

        @JsonProperty("uri")
        String uri;

        @Override
        public String toString() {
            return "ResourceReference{" + "type='" + type + '\'' + ", uri='" + uri + '\'' + '}';
        }

        public ResourceReference(String uri) {
            this.uri = uri;
            this.type = "ref/resource";
        }

        public ResourceReference(String type, String uri) {
            this.type = type;
            this.uri = uri;
        }

        @Override
        public String type() {
            return this.type;
        }

        @Override
        public String identifier() {
            return this.uri;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
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
        @JsonSubTypes.Type(value = TextContent.class, name = "text"),
        @JsonSubTypes.Type(value = ImageContent.class, name = "image"),
        @JsonSubTypes.Type(value = EmbeddedResource.class, name = "resource")
    })
    public interface Content {

        default String type() {
            if (this instanceof TextContent) {
                return "text";
            } else if (this instanceof ImageContent) {
                return "image";
            } else if (this instanceof EmbeddedResource) {
                return "resource";
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

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageContent implements Content { // @formatter:on
        @JsonProperty("audience")
        List<Role> audience;

        @JsonProperty("priority")
        Double priority;

        @JsonProperty("data")
        String data;

        @JsonProperty("mimeType")
        String mimeType;

        @Override
        public String toString() {
            return "ImageContent{" + "audience="
                    + audience + ", priority="
                    + priority + ", data='"
                    + data + '\'' + ", mimeType='"
                    + mimeType + '\'' + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ImageContent that = (ImageContent) o;
            return Objects.equals(audience, that.audience)
                    && Objects.equals(priority, that.priority)
                    && Objects.equals(data, that.data)
                    && Objects.equals(mimeType, that.mimeType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(audience, priority, data, mimeType);
        }

        public ImageContent() {}

        public ImageContent(List<Role> audience, Double priority, String data, String mimeType) {
            this.audience = audience;
            this.priority = priority;
            this.data = data;
            this.mimeType = mimeType;
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

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddedResource implements Content { // @formatter:on
        @JsonProperty("audience")
        List<Role> audience;

        @JsonProperty("priority")
        Double priority;

        @JsonProperty("resource")
        ResourceContents resource;

        @Override
        public String toString() {
            return "EmbeddedResource{" + "audience="
                    + audience + ", priority="
                    + priority + ", resource="
                    + resource + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            EmbeddedResource that = (EmbeddedResource) o;
            return Objects.equals(audience, that.audience)
                    && Objects.equals(priority, that.priority)
                    && Objects.equals(resource, that.resource);
        }

        @Override
        public int hashCode() {
            return Objects.hash(audience, priority, resource);
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

        public ResourceContents getResource() {
            return resource;
        }

        public void setResource(ResourceContents resource) {
            this.resource = resource;
        }

        public EmbeddedResource() {}

        public EmbeddedResource(List<Role> audience, Double priority, ResourceContents resource) {
            this.audience = audience;
            this.priority = priority;
            this.resource = resource;
        }
    }

    // ---------------------------
    // Roots
    // ---------------------------
    /**
     * Represents a root directory or file that the server can operate on.
     * uri The URI identifying the root. This *must* start with file:// for now.
     * This restriction may be relaxed in future versions of the protocol to allow other
     * URI schemes.
     * name An optional name for the root. This can be used to provide a
     * human-readable identifier for the root, which may be useful for display purposes or
     * for referencing the root in other parts of the application.
     */
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

    /**
     * The client's response to a roots/list request from the server. This result contains
     * an array of Root objects, each representing a root directory or file that the
     * server can operate on.
     * roots An array of Root objects, each representing a root directory or file
     * that the server can operate on.
     */
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
