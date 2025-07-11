/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	private McpSchema() {
	}

	public static final String LATEST_PROTOCOL_VERSION = "2025-03-26";

	public static final String JSONRPC_VERSION = "2.0";

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

	// Resources Methods
	public static final String METHOD_RESOURCES_LIST = "resources/list";

	public static final String METHOD_RESOURCES_READ = "resources/read";

	public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

	public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

	public static final String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";

	public static final String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

	// Prompt Methods
	public static final String METHOD_PROMPT_LIST = "prompts/list";

	public static final String METHOD_PROMPT_GET = "prompts/get";

	public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	// Logging Methods
	public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

	public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

	// Roots Methods
	public static final String METHOD_ROOTS_LIST = "roots/list";

	public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

	// Sampling Methods
	public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

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
	}

	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

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
		}
		else if (map.containsKey("method") && !map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCNotification.class);
		}
		else if (map.containsKey("result") || map.containsKey("error")) {
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
		@JsonProperty("jsonrpc") String jsonrpc;
		@JsonProperty("method") String method;
		@JsonProperty("id") Object id;
		@JsonProperty("params") Object params;

		public JSONRPCRequest(String jsonrpc, String method, Object id, Object params) {
			this.jsonrpc = jsonrpc;
			this.method = method;
			this.id = id;
			this.params = params;
		}

		public JSONRPCRequest() {
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
			return "JSONRPCRequest{" +
					"jsonrpc='" + jsonrpc + '\'' +
					", method='" + method + '\'' +
					", id=" + id +
					", params=" + params +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			JSONRPCRequest that = (JSONRPCRequest) o;
			return Objects.equals(jsonrpc, that.jsonrpc) && Objects.equals(method, that.method) && Objects.equals(id, that.id) && Objects.equals(params, that.params);
		}

		@Override
		public int hashCode() {
			return Objects.hash(jsonrpc, method, id, params);
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCNotification implements JSONRPCMessage {
		@JsonProperty("jsonrpc") String jsonrpc;
		@JsonProperty("method") String method;
		@JsonProperty("params") Map<String, Object> params;

		public JSONRPCNotification() {
		}

		public JSONRPCNotification(String jsonrpc, String method, Map<String, Object> params) {
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

		public Map<String, Object> getParams() {
			return params;
		}

		public void setParams(Map<String, Object> params) {
			this.params = params;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			JSONRPCNotification that = (JSONRPCNotification) o;
			return Objects.equals(jsonrpc, that.jsonrpc) && Objects.equals(method, that.method) && Objects.equals(params, that.params);
		}

		@Override
		public int hashCode() {
			return Objects.hash(jsonrpc, method, params);
		}

		@Override
		public String toString() {
			return "JSONRPCNotification{" +
					"jsonrpc='" + jsonrpc + '\'' +
					", method='" + method + '\'' +
					", params=" + params +
					'}';
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCResponse implements JSONRPCMessage {
		@JsonProperty("jsonrpc") String jsonrpc;
		@JsonProperty("id") Object id;
		@JsonProperty("result") Object result;
		@JsonProperty("error") JSONRPCError error;

		public JSONRPCResponse(String jsonrpc, Object id, Object result, JSONRPCError error) {
			this.jsonrpc = jsonrpc;
			this.id = id;
			this.result = result;
			this.error = error;
		}

		public JSONRPCResponse() {
		}

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
			return "JSONRPCResponse{" +
					"jsonrpc='" + jsonrpc + '\'' +
					", id=" + id +
					", result=" + result +
					", error=" + error +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			JSONRPCResponse that = (JSONRPCResponse) o;
			return Objects.equals(jsonrpc, that.jsonrpc) && Objects.equals(id, that.id) && Objects.equals(result, that.result) && Objects.equals(error, that.error);
		}

		@Override
		public int hashCode() {
			return Objects.hash(jsonrpc, id, result, error);
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class JSONRPCError {
			@JsonProperty("code") int code;
			@JsonProperty("message") String message;
			@JsonProperty("data") Object data;

			public JSONRPCError(int code, String message, Object data) {
				this.code = code;
				this.message = message;
				this.data = data;
			}

			public JSONRPCError() {
			}

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
				return "JSONRPCError{" +
						"code=" + code +
						", message='" + message + '\'' +
						", data=" + data +
						'}';
			}
		}
	}// @formatter:on

	// ---------------------------
	// Initialization
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitializeRequest implements Request {
		@JsonProperty("protocolVersion") String protocolVersion;
		@JsonProperty("capabilities") ClientCapabilities capabilities;
		@JsonProperty("clientInfo") Implementation clientInfo;

		public InitializeRequest(String protocolVersion, ClientCapabilities capabilities, Implementation clientInfo) {
			this.protocolVersion = protocolVersion;
			this.capabilities = capabilities;
			this.clientInfo = clientInfo;
		}

		public InitializeRequest() {
		}

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

		@Override
		public String toString() {
			return "InitializeRequest{" +
					"protocolVersion='" + protocolVersion + '\'' +
					", capabilities=" + capabilities +
					", clientInfo=" + clientInfo +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			InitializeRequest that = (InitializeRequest) o;
			return Objects.equals(protocolVersion, that.protocolVersion) && Objects.equals(capabilities, that.capabilities) && Objects.equals(clientInfo, that.clientInfo);
		}

		@Override
		public int hashCode() {
			return Objects.hash(protocolVersion, capabilities, clientInfo);
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitializeResult {
		@JsonProperty("protocolVersion") String protocolVersion;
		@JsonProperty("capabilities") ServerCapabilities capabilities;
		@JsonProperty("serverInfo") Implementation serverInfo;
		@JsonProperty("instructions") String instructions;

		public InitializeResult() {
		}

		@Override
		public String toString() {
			return "InitializeResult{" +
					"protocolVersion='" + protocolVersion + '\'' +
					", capabilities=" + capabilities +
					", serverInfo=" + serverInfo +
					", instructions='" + instructions + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			InitializeResult that = (InitializeResult) o;
			return Objects.equals(protocolVersion, that.protocolVersion) && Objects.equals(capabilities, that.capabilities) && Objects.equals(serverInfo, that.serverInfo) && Objects.equals(instructions, that.instructions);
		}

		@Override
		public int hashCode() {
			return Objects.hash(protocolVersion, capabilities, serverInfo, instructions);
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

		public InitializeResult(String protocolVersion, ServerCapabilities capabilities, Implementation serverInfo, String instructions) {
			this.protocolVersion = protocolVersion;
			this.capabilities = capabilities;
			this.serverInfo = serverInfo;
			this.instructions = instructions;
		}
	} // @formatter:on

	/**
	 * Clients can implement additional features to enrich connected MCP servers with
	 * additional capabilities. These capabilities can be used to extend the functionality
	 * of the server, or to provide additional information to the server about the
	 * client's capabilities.
	 *
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
		@JsonProperty("experimental") Map<String, Object> experimental;
		@JsonProperty("roots") RootCapabilities roots;
		@JsonProperty("sampling") Sampling sampling;


		public ClientCapabilities(Map<String, Object> experimental, RootCapabilities roots, Sampling sampling) {
			this.experimental = experimental;
			this.roots = roots;
			this.sampling = sampling;
		}

		public ClientCapabilities() {
		}

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

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ClientCapabilities that = (ClientCapabilities) o;
			return Objects.equals(experimental, that.experimental) && Objects.equals(roots, that.roots) && Objects.equals(sampling, that.sampling);
		}

		@Override
		public int hashCode() {
			return Objects.hash(experimental, roots, sampling);
		}

		@Override
		public String toString() {
			return "ClientCapabilities{" +
					"experimental=" + experimental +
					", roots=" + roots +
					", sampling=" + sampling +
					'}';
		}

		/**
		 * Roots define the boundaries of where servers can operate within the filesystem,
		 * allowing them to understand which directories and files they have access to.
		 * Servers can request the list of roots from supporting clients and
		 * receive notifications when that list changes.
		 *
		 * listChanged Whether the client would send notification about roots
		 * 		  has changed since the last time the server checked.
		 */
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class RootCapabilities {
			@JsonProperty("listChanged") Boolean listChanged;

			public RootCapabilities() {
			}

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
				return "RootCapabilities{" +
						"listChanged=" + listChanged +
						'}';
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
		public static class Sampling {
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private Map<String, Object> experimental;
			private RootCapabilities roots;
			private Sampling sampling;

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass()) return false;
				Builder builder = (Builder) o;
				return Objects.equals(experimental, builder.experimental) && Objects.equals(roots, builder.roots) && Objects.equals(sampling, builder.sampling);
			}

			@Override
			public int hashCode() {
				return Objects.hash(experimental, roots, sampling);
			}

			public Builder() {
			}

			public Builder(Map<String, Object> experimental, RootCapabilities roots, Sampling sampling) {
				this.experimental = experimental;
				this.roots = roots;
				this.sampling = sampling;
			}

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

			@Override
			public String toString() {
				return "Builder{" +
						"experimental=" + experimental +
						", roots=" + roots +
						", sampling=" + sampling +
						'}';
			}

			public ClientCapabilities build() {
				return new ClientCapabilities(experimental, roots, sampling);
			}
		}
	}// @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ServerCapabilities {
		@JsonProperty("experimental") Map<String, Object> experimental;
		@JsonProperty("logging") LoggingCapabilities logging;
		@JsonProperty("prompts") PromptCapabilities prompts;
		@JsonProperty("resources") ResourceCapabilities resources;
		@JsonProperty("tools") ToolCapabilities tools;

		public ServerCapabilities(Map<String, Object> experimental, LoggingCapabilities logging, PromptCapabilities prompts, ResourceCapabilities resources, ToolCapabilities tools) {
			this.experimental = experimental;
			this.logging = logging;
			this.prompts = prompts;
			this.resources = resources;
			this.tools = tools;
		}

		public ServerCapabilities() {
		}

		public Map<String, Object> getExperimental() {
			return experimental;
		}

		public void setExperimental(Map<String, Object> experimental) {
			this.experimental = experimental;
		}

		public LoggingCapabilities getLogging() {
			return logging;
		}

		public void setLogging(LoggingCapabilities logging) {
			this.logging = logging;
		}

		public PromptCapabilities getPrompts() {
			return prompts;
		}

		public void setPrompts(PromptCapabilities prompts) {
			this.prompts = prompts;
		}

		public ResourceCapabilities getResources() {
			return resources;
		}

		public void setResources(ResourceCapabilities resources) {
			this.resources = resources;
		}

		public ToolCapabilities getTools() {
			return tools;
		}

		public void setTools(ToolCapabilities tools) {
			this.tools = tools;
		}

		@Override
		public String toString() {
			return "ServerCapabilities{" +
					"experimental=" + experimental +
					", logging=" + logging +
					", prompts=" + prompts +
					", resources=" + resources +
					", tools=" + tools +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ServerCapabilities that = (ServerCapabilities) o;
			return Objects.equals(experimental, that.experimental) && Objects.equals(logging, that.logging) && Objects.equals(prompts, that.prompts) && Objects.equals(resources, that.resources) && Objects.equals(tools, that.tools);
		}

		@Override
		public int hashCode() {
			return Objects.hash(experimental, logging, prompts, resources, tools);
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class LoggingCapabilities {
		}
	
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class PromptCapabilities {
			@JsonProperty("listChanged") Boolean listChanged;

			public PromptCapabilities(Boolean listChanged) {
				this.listChanged = listChanged;
			}

			public PromptCapabilities() {
			}

			public Boolean getListChanged() {
				return listChanged;
			}

			public void setListChanged(Boolean listChanged) {
				this.listChanged = listChanged;
			}

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass()) return false;
				PromptCapabilities that = (PromptCapabilities) o;
				return Objects.equals(listChanged, that.listChanged);
			}

			@Override
			public int hashCode() {
				return Objects.hashCode(listChanged);
			}

			@Override
			public String toString() {
				return "PromptCapabilities{" +
						"listChanged=" + listChanged +
						'}';
			}
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class ResourceCapabilities {
			@JsonProperty("subscribe") Boolean subscribe;
			@JsonProperty("listChanged") Boolean listChanged;

			public ResourceCapabilities() {
			}

			public ResourceCapabilities(Boolean subscribe, Boolean listChanged) {
				this.subscribe = subscribe;
				this.listChanged = listChanged;
			}

			public Boolean getSubscribe() {
				return subscribe;
			}

			public void setSubscribe(Boolean subscribe) {
				this.subscribe = subscribe;
			}

			public Boolean getListChanged() {
				return listChanged;
			}

			public void setListChanged(Boolean listChanged) {
				this.listChanged = listChanged;
			}

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass()) return false;
				ResourceCapabilities that = (ResourceCapabilities) o;
				return Objects.equals(subscribe, that.subscribe) && Objects.equals(listChanged, that.listChanged);
			}

			@Override
			public int hashCode() {
				return Objects.hash(subscribe, listChanged);
			}

			@Override
			public String toString() {
				return "ResourceCapabilities{" +
						"subscribe=" + subscribe +
						", listChanged=" + listChanged +
						'}';
			}
		}

		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class ToolCapabilities {
			@JsonProperty("listChanged") Boolean listChanged;

			public ToolCapabilities() {
			}

			public ToolCapabilities(Boolean listChanged) {
				this.listChanged = listChanged;
			}

			public Boolean getListChanged() {
				return listChanged;
			}

			public void setListChanged(Boolean listChanged) {
				this.listChanged = listChanged;
			}

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass()) return false;
				ToolCapabilities that = (ToolCapabilities) o;
				return Objects.equals(listChanged, that.listChanged);
			}

			@Override
			public int hashCode() {
				return Objects.hashCode(listChanged);
			}

			@Override
			public String toString() {
				return "ToolCapabilities{" +
						"listChanged=" + listChanged +
						'}';
			}
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private Map<String, Object> experimental;
			private LoggingCapabilities logging = new LoggingCapabilities();
			private PromptCapabilities prompts;
			private ResourceCapabilities resources;
			private ToolCapabilities tools;

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
				return new ServerCapabilities(experimental, logging, prompts, resources, tools);
			}
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Implementation {
		@JsonProperty("name") String name;
		@JsonProperty("version") String version;

		public Implementation() {
		}

		public Implementation(String name, String version) {
			this.name = name;
			this.version = version;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			Implementation that = (Implementation) o;
			return Objects.equals(name, that.name) && Objects.equals(version, that.version);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, version);
		}

		@Override
		public String toString() {
			return "Implementation{" +
					"name='" + name + '\'' +
					", version='" + version + '\'' +
					'}';
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
	public enum Role {// @formatter:off

		@JsonProperty("user") USER,
		@JsonProperty("assistant") ASSISTANT
	}// @formatter:on

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
	 *
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
		@JsonProperty("audience") List<Role> audience;
		@JsonProperty("priority") Double priority;

		public Annotations() {
		}

		public Annotations(Double priority, List<Role> audience) {
			this.priority = priority;
			this.audience = audience;
		}

		@Override
		public String toString() {
			return "Annotations{" +
					"audience=" + audience +
					", priority=" + priority +
					'}';
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
	 * A known resource that the server is capable of reading.
	 *
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
	public static class Resource implements Annotated {
		@JsonProperty("uri") String uri;
		@JsonProperty("name") String name;
		@JsonProperty("description") String description;
		@JsonProperty("mimeType") String mimeType;
		@JsonProperty("annotations") Annotations annotations;

		public Resource() {
		}

		public Resource(String uri, String name, String description, String mimeType, Annotations annotations) {
			this.uri = uri;
			this.name = name;
			this.description = description;
			this.mimeType = mimeType;
			this.annotations = annotations;
		}

		@Override
		public String toString() {
			return "Resource{" +
					"uri='" + uri + '\'' +
					", name='" + name + '\'' +
					", description='" + description + '\'' +
					", mimeType='" + mimeType + '\'' +
					", annotations=" + annotations +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			Resource resource = (Resource) o;
			return Objects.equals(uri, resource.uri) && Objects.equals(name, resource.name) && Objects.equals(description, resource.description) && Objects.equals(mimeType, resource.mimeType) && Objects.equals(annotations, resource.annotations);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri, name, description, mimeType, annotations);
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
		public Annotations getAnnotations() {
			return annotations;
		}

		public void setAnnotations(Annotations annotations) {
			this.annotations = annotations;
		}
	} // @formatter:on

	/**
	 * Resource templates allow servers to expose parameterized resources using URI
	 * templates.
	 *
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
	public static class ResourceTemplate implements Annotated {
		@JsonProperty("uriTemplate") String uriTemplate;
		@JsonProperty("name") String name;
		@JsonProperty("description") String description;
		@JsonProperty("mimeType") String mimeType;
		@JsonProperty("annotations") Annotations annotations;

		@Override
		public String toString() {
			return "ResourceTemplate{" +
					"uriTemplate='" + uriTemplate + '\'' +
					", name='" + name + '\'' +
					", description='" + description + '\'' +
					", mimeType='" + mimeType + '\'' +
					", annotations=" + annotations +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ResourceTemplate that = (ResourceTemplate) o;
			return Objects.equals(uriTemplate, that.uriTemplate) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(mimeType, that.mimeType) && Objects.equals(annotations, that.annotations);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uriTemplate, name, description, mimeType, annotations);
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
		public Annotations getAnnotations() {
			return annotations;
		}

		public void setAnnotations(Annotations annotations) {
			this.annotations = annotations;
		}

		public ResourceTemplate() {
		}

		public ResourceTemplate(String uriTemplate, String name, String description, String mimeType, Annotations annotations) {
			this.uriTemplate = uriTemplate;
			this.name = name;
			this.description = description;
			this.mimeType = mimeType;
			this.annotations = annotations;
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListResourcesResult {
		@JsonProperty("resources") List<Resource> resources;
		@JsonProperty("nextCursor") String nextCursor;

		@Override
		public String toString() {
			return "ListResourcesResult{" +
					"resources=" + resources +
					", nextCursor='" + nextCursor + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ListResourcesResult that = (ListResourcesResult) o;
			return Objects.equals(resources, that.resources) && Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(resources, nextCursor);
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

		public ListResourcesResult() {
		}

		public ListResourcesResult(List<Resource> resources, String nextCursor) {
			this.resources = resources;
			this.nextCursor = nextCursor;
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListResourceTemplatesResult {
		@JsonProperty("resourceTemplates") List<ResourceTemplate> resourceTemplates;
		@JsonProperty("nextCursor") String nextCursor;

		@Override
		public String toString() {
			return "ListResourceTemplatesResult{" +
					"resourceTemplates=" + resourceTemplates +
					", nextCursor='" + nextCursor + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ListResourceTemplatesResult that = (ListResourceTemplatesResult) o;
			return Objects.equals(resourceTemplates, that.resourceTemplates) && Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(resourceTemplates, nextCursor);
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

		public ListResourceTemplatesResult() {
		}

		public ListResourceTemplatesResult(String nextCursor, List<ResourceTemplate> resourceTemplates) {
			this.nextCursor = nextCursor;
			this.resourceTemplates = resourceTemplates;
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ReadResourceRequest {
		@JsonProperty("uri") String uri;

		@Override
		public String toString() {
			return "ReadResourceRequest{" +
					"uri='" + uri + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ReadResourceRequest that = (ReadResourceRequest) o;
			return Objects.equals(uri, that.uri);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(uri);
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public ReadResourceRequest() {
		}

		public ReadResourceRequest(String uri) {
			this.uri = uri;
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ReadResourceResult{
		@JsonProperty("contents") List<ResourceContents> contents;

		@Override
		public String toString() {
			return "ReadResourceResult{" +
					"contents=" + contents +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ReadResourceResult that = (ReadResourceResult) o;
			return Objects.equals(contents, that.contents);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(contents);
		}

		public List<ResourceContents> getContents() {
			return contents;
		}

		public void setContents(List<ResourceContents> contents) {
			this.contents = contents;
		}

		public ReadResourceResult() {
		}

		public ReadResourceResult(List<ResourceContents> contents) {
			this.contents = contents;
		}
	} // @formatter:on

	/**
	 * Sent from the client to request resources/updated notifications from the server
	 * whenever a particular resource changes.
	 *
	 * uri the URI of the resource to subscribe to. The URI can use any protocol;
	 * it is up to the server how to interpret it.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SubscribeRequest{
		@JsonProperty("uri") String uri;

		@Override
		public String toString() {
			return "SubscribeRequest{" +
					"uri='" + uri + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			SubscribeRequest that = (SubscribeRequest) o;
			return Objects.equals(uri, that.uri);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(uri);
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public SubscribeRequest() {
		}

		public SubscribeRequest(String uri) {
			this.uri = uri;
		}
	} // @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UnsubscribeRequest{
		@JsonProperty("uri") String uri;

		@Override
		public String toString() {
			return "UnsubscribeRequest{" +
					"uri='" + uri + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			UnsubscribeRequest that = (UnsubscribeRequest) o;
			return Objects.equals(uri, that.uri);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(uri);
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public UnsubscribeRequest() {
		}

		public UnsubscribeRequest(String uri) {
			this.uri = uri;
		}
	} // @formatter:on

	/**
	 * The contents of a specific resource or sub-resource.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, include = As.PROPERTY)
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextResourceContents.class, name = "text"),
			@JsonSubTypes.Type(value = BlobResourceContents.class, name = "blob") })
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

	}

	/**
	 * Text contents of a resource.
	 *
	 * uri the URI of this resource.
	 * mimeType the MIME type of this resource.
	 * text the text of the resource. This must only be set if the resource can
	 * actually be represented as text (not binary data).
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TextResourceContents implements ResourceContents {
		@JsonProperty("uri") String uri;
		@JsonProperty("mimeType") String mimeType;
		@JsonProperty("text") String text;

		@Override
		public String toString() {
			return "TextResourceContents{" +
					"uri='" + uri + '\'' +
					", mimeType='" + mimeType + '\'' +
					", text='" + text + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			TextResourceContents that = (TextResourceContents) o;
			return Objects.equals(uri, that.uri) && Objects.equals(mimeType, that.mimeType) && Objects.equals(text, that.text);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri, mimeType, text);
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

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public TextResourceContents() {
		}

		public TextResourceContents(String text, String mimeType, String uri) {
			this.text = text;
			this.mimeType = mimeType;
			this.uri = uri;
		}
	} // @formatter:on

	/**
	 * Binary contents of a resource.
	 *
	 * uri the URI of this resource.
	 * mimeType the MIME type of this resource.
	 * blob a base64-encoded string representing the binary data of the resource.
	 * This must only be set if the resource can actually be represented as binary data
	 * (not text).
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BlobResourceContents implements ResourceContents {
		@JsonProperty("uri") String uri;
		@JsonProperty("mimeType") String mimeType;
		@JsonProperty("blob") String blob;

		@Override
		public String toString() {
			return "BlobResourceContents{" +
					"uri='" + uri + '\'' +
					", mimeType='" + mimeType + '\'' +
					", blob='" + blob + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			BlobResourceContents that = (BlobResourceContents) o;
			return Objects.equals(uri, that.uri) && Objects.equals(mimeType, that.mimeType) && Objects.equals(blob, that.blob);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri, mimeType, blob);
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

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public String getBlob() {
			return blob;
		}

		public void setBlob(String blob) {
			this.blob = blob;
		}

		public BlobResourceContents() {
		}

		public BlobResourceContents(String uri, String mimeType, String blob) {
			this.uri = uri;
			this.mimeType = mimeType;
			this.blob = blob;
		}
	} // @formatter:on

	// ---------------------------
	// Prompt Interfaces
	// ---------------------------
	/**
	 * A prompt or prompt template that the server offers.
	 *
	 * name The name of the prompt or prompt template.
	 * description An optional description of what this prompt provides.
	 * arguments A list of arguments to use for templating the prompt.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Prompt {
		@JsonProperty("name") String name;
		@JsonProperty("description") String description;
		@JsonProperty("arguments") List<PromptArgument> arguments;

		@Override
		public String toString() {
			return "Prompt{" +
					"name='" + name + '\'' +
					", description='" + description + '\'' +
					", arguments=" + arguments +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			Prompt prompt = (Prompt) o;
			return Objects.equals(name, prompt.name) && Objects.equals(description, prompt.description) && Objects.equals(arguments, prompt.arguments);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, description, arguments);
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

		public Prompt() {
		}

		public Prompt(List<PromptArgument> arguments, String description, String name) {
			this.arguments = arguments;
			this.description = description;
			this.name = name;
		}
	} // @formatter:on

	/**
	 * Describes an argument that a prompt can accept.
	 *
	 * name The name of the argument.
	 * description A human-readable description of the argument.
	 * required Whether this argument must be provided.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptArgument {
		@JsonProperty("name") String name;
		@JsonProperty("description") String description;
		@JsonProperty("required") Boolean required;

		@Override
		public String toString() {
			return "PromptArgument{" +
					"name='" + name + '\'' +
					", description='" + description + '\'' +
					", required=" + required +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			PromptArgument that = (PromptArgument) o;
			return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(required, that.required);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, description, required);
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

		public PromptArgument() {
		}

		public PromptArgument(String name, String description, Boolean required) {
			this.name = name;
			this.description = description;
			this.required = required;
		}
	}// @formatter:on

	/**
	 * Describes a message returned as part of a prompt.
	 *
	 * This is similar to `SamplingMessage`, but also supports the embedding of resources
	 * from the MCP server.
	 *
	 * role The sender or recipient of messages and data in a conversation.
	 * content The content of the message of type {@link Content}.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptMessage {
		@JsonProperty("role") Role role;
		@JsonProperty("content") Content content;

		@Override
		public String toString() {
			return "PromptMessage{" +
					"role=" + role +
					", content=" + content +
					'}';
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

		public PromptMessage() {
		}

		public PromptMessage(Role role, Content content) {
			this.role = role;
			this.content = content;
		}
	} // @formatter:on

	/**
	 * The server's response to a prompts/list request from the client.
	 *
	 * prompts A list of prompts that the server provides.
	 * nextCursor An optional cursor for pagination. If present, indicates there
	 * are more prompts available.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListPromptsResult {
		@JsonProperty("prompts") List<Prompt> prompts;
		@JsonProperty("nextCursor") String nextCursor;

		@Override
		public String toString() {
			return "ListPromptsResult{" +
					"prompts=" + prompts +
					", nextCursor='" + nextCursor + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ListPromptsResult that = (ListPromptsResult) o;
			return Objects.equals(prompts, that.prompts) && Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(prompts, nextCursor);
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

		public ListPromptsResult() {
		}

		public ListPromptsResult(List<Prompt> prompts, String nextCursor) {
			this.prompts = prompts;
			this.nextCursor = nextCursor;
		}
	}// @formatter:on

	/**
	 * Used by the client to get a prompt provided by the server.
	 *
	 * name The name of the prompt or prompt template.
	 * arguments Arguments to use for templating the prompt.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)

	public static class GetPromptRequest implements Request {
		@JsonProperty("name") String name;
		@JsonProperty("arguments") Map<String, Object> arguments;

		@Override
		public String toString() {
			return "GetPromptRequest{" +
					"name='" + name + '\'' +
					", arguments=" + arguments +
					'}';
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

		public GetPromptRequest() {
		}

		public GetPromptRequest(String name, Map<String, Object> arguments) {
			this.name = name;
			this.arguments = arguments;
		}}// @formatter:off

	/**
	 * The server's response to a prompts/get request from the client.
	 *
	 * description An optional description for the prompt.
	 * messages A list of messages to display as part of the prompt.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GetPromptResult {
		@JsonProperty("description") String description;
		@JsonProperty("messages") List<PromptMessage> messages;
		public GetPromptResult() {
		}
		public GetPromptResult(String description, List<PromptMessage> messages) {
			this.description = description;
			this.messages = messages;
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

		@Override public String toString() {
			return "GetPromptResult{" +
					"description='" + description + '\'' +
					", messages=" + messages +
					'}';
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
	} // @formatter:on

	// ---------------------------
	// Tool Interfaces
	// ---------------------------
	/**
	 * The server's response to a tools/list request from the client.
	 *
	 * tools A list of tools that the server provides.
	 * nextCursor An optional cursor for pagination. If present, indicates there
	 * are more tools available.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListToolsResult {
		@JsonProperty("tools") List<Tool> tools;
		@JsonProperty("nextCursor") String nextCursor;

		@Override
		public String toString() {
			return "ListToolsResult{" +
					"tools=" + tools +
					", nextCursor='" + nextCursor + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ListToolsResult that = (ListToolsResult) o;
			return Objects.equals(tools, that.tools) && Objects.equals(nextCursor, that.nextCursor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(tools, nextCursor);
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

		public ListToolsResult(List<Tool> tools, String nextCursor) {
			this.tools = tools;
			this.nextCursor = nextCursor;
		}

		public ListToolsResult() {
		}
	}// @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JsonSchema {
		@JsonProperty("type") String type;
		@JsonProperty("properties") Map<String, Object> properties;
		@JsonProperty("required") List<String> required;
		@JsonProperty("additionalProperties") Boolean additionalProperties;

		@Override
		public String toString() {
			return "JsonSchema{" +
					"type='" + type + '\'' +
					", properties=" + properties +
					", required=" + required +
					", additionalProperties=" + additionalProperties +
					'}';
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

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			JsonSchema that = (JsonSchema) o;
			return Objects.equals(type, that.type) && Objects.equals(properties, that.properties) && Objects.equals(required, that.required) && Objects.equals(additionalProperties, that.additionalProperties);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, properties, required, additionalProperties);
		}

		public JsonSchema() {
		}

		public JsonSchema(String type, Map<String, Object> properties, List<String> required, Boolean additionalProperties) {
			this.type = type;
			this.properties = properties;
			this.required = required;
			this.additionalProperties = additionalProperties;
		}
	} // @formatter:on

	/**
	 * Represents a tool that the server provides. Tools enable servers to expose
	 * executable functionality to the system. Through these tools, you can interact with
	 * external systems, perform computations, and take actions in the real world.
	 *
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
		@JsonProperty("name") String name;
		@JsonProperty("description") String description;
		@JsonProperty("returnDirect") Boolean returnDirect;
		@JsonProperty("inputSchema") JsonSchema inputSchema;
		@JsonProperty("outputSchema") JsonSchema outputSchema;

		public Tool(String name, String description, Boolean returnDirect, String inSchemaJson, String outSchemaJson) {
			this(name, description, returnDirect, parseSchema(inSchemaJson), (outSchemaJson == null || outSchemaJson.isEmpty()) ?  null : parseSchema(outSchemaJson));
		}

		@Override
		public String toString() {
			return "Tool{" +
					"name='" + name + '\'' +
					", description='" + description + '\'' +
					", returnDirect=" + returnDirect +
					", inputSchema=" + inputSchema +
					", outputSchema=" + outputSchema +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			Tool tool = (Tool) o;
			return Objects.equals(name, tool.name) && Objects.equals(description, tool.description) && Objects.equals(returnDirect, tool.returnDirect) && Objects.equals(inputSchema, tool.inputSchema) && Objects.equals(outputSchema, tool.outputSchema);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, description, returnDirect, inputSchema, outputSchema);
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

		public Boolean getReturnDirect() {
			return returnDirect;
		}

		public void setReturnDirect(Boolean returnDirect) {
			this.returnDirect = returnDirect;
		}

		public JsonSchema getInputSchema() {
			return inputSchema;
		}

		public void setInputSchema(JsonSchema inputSchema) {
			this.inputSchema = inputSchema;
		}

		public JsonSchema getOutputSchema() {
			return outputSchema;
		}

		public void setOutputSchema(JsonSchema outputSchema) {
			this.outputSchema = outputSchema;
		}

		public Tool() {
		}

		public Tool(String name, String description, Boolean returnDirect, JsonSchema inputSchema, JsonSchema outputSchema) {
			this.name = name;
			this.description = description;
			this.returnDirect = returnDirect;
			this.inputSchema = inputSchema;
			this.outputSchema = outputSchema;
		}
	} // @formatter:on

	private static JsonSchema parseSchema(String schema) {
		try {
			return OBJECT_MAPPER.readValue(schema, JsonSchema.class);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Invalid schema: " + schema, e);
		}
	}

	/**
	 * Used by the client to call a tool provided by the server.
	 *
	 * name The name of the tool to call. This must match a tool name from
	 * tools/list.
	 * Arguments to pass to the tool. These must conform to the tool's
	 * input schema.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CallToolRequest implements Request {
		@JsonProperty("name") String name;
		@JsonProperty("arguments") Map<String, Object> arguments;

		@Override
		public String toString() {
			return "CallToolRequest{" +
					"name='" + name + '\'' +
					", arguments=" + arguments +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			CallToolRequest that = (CallToolRequest) o;
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

		public CallToolRequest() {
		}

		public CallToolRequest(String name, Map<String, Object> arguments) {
			this.name = name;
			this.arguments = arguments;
		}}// @formatter:off

	/**
	 * The server's response to a tools/call request from the client.
	 *
	 * content A list of content items representing the tool's output. Each item can be text, an image,
	 *                or an embedded resource.
	 * isError If true, indicates that the tool execution failed and the content contains error information.
	 *                If false or absent, indicates successful execution.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CallToolResult {
		@JsonProperty("content") List<Content> content;
		@JsonProperty("isError") Boolean isError;
		public CallToolResult(List<Content> content, Boolean isError) {
			this.content = content;
			this.isError = isError;
		}

		public CallToolResult() {
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

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			CallToolResult that = (CallToolResult) o;
			return Objects.equals(content, that.content) && Objects.equals(isError, that.isError);
		}
		@Override
		public int hashCode() {
			return Objects.hash(content, isError);
		}

		@Override
		public String toString() {
			return "CallToolResult{" +
					"content=" + content +
					", isError=" + isError +
					'}';
		}
	} // @formatter:on

	// ---------------------------
	// Sampling Interfaces
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ModelPreferences {
		@JsonProperty("hints") List<ModelHint> hints;
		@JsonProperty("costPriority") Double costPriority;
		@JsonProperty("speedPriority") Double speedPriority;
		@JsonProperty("intelligencePriority") Double intelligencePriority;

		@Override
		public String toString() {
			return "ModelPreferences{" +
					"hints=" + hints +
					", costPriority=" + costPriority +
					", speedPriority=" + speedPriority +
					", intelligencePriority=" + intelligencePriority +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ModelPreferences that = (ModelPreferences) o;
			return Objects.equals(hints, that.hints) && Objects.equals(costPriority, that.costPriority) && Objects.equals(speedPriority, that.speedPriority) && Objects.equals(intelligencePriority, that.intelligencePriority);
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

		public ModelPreferences() {
		}

		public ModelPreferences(List<ModelHint> hints, Double costPriority, Double speedPriority, Double intelligencePriority) {
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
		@JsonProperty("name") String name;

		@Override
		public String toString() {
			return "ModelHint{" +
					"name='" + name + '\'' +
					'}';
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

		public ModelHint() {
		}

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
		@JsonProperty("role") Role role;
		@JsonProperty("content") Content content;

		@Override
		public String toString() {
			return "SamplingMessage{" +
					"role=" + role +
					", content=" + content +
					'}';
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

		public SamplingMessage() {
		}

		public SamplingMessage(Role role, Content content) {
			this.role = role;
			this.content = content;
		}
	} // @formatter:on

	// Sampling and Message Creation
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CreateMessageRequest implements Request {
		@JsonProperty("messages") List<SamplingMessage> messages;
		@JsonProperty("modelPreferences") ModelPreferences modelPreferences;
		@JsonProperty("systemPrompt") String systemPrompt;
		@JsonProperty("includeContext") ContextInclusionStrategy includeContext;
		@JsonProperty("temperature") Double temperature;
		@JsonProperty("maxTokens") int maxTokens;
		@JsonProperty("stopSequences") List<String> stopSequences;
		@JsonProperty("metadata") Map<String, Object> metadata;

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			CreateMessageRequest that = (CreateMessageRequest) o;
			return maxTokens == that.maxTokens && Objects.equals(messages, that.messages) && Objects.equals(modelPreferences, that.modelPreferences) && Objects.equals(systemPrompt, that.systemPrompt) && includeContext == that.includeContext && Objects.equals(temperature, that.temperature) && Objects.equals(stopSequences, that.stopSequences) && Objects.equals(metadata, that.metadata);
		}

		@Override
		public int hashCode() {
			return Objects.hash(messages, modelPreferences, systemPrompt, includeContext, temperature, maxTokens, stopSequences, metadata);
		}

		@Override
		public String toString() {
			return "CreateMessageRequest{" +
					"messages=" + messages +
					", modelPreferences=" + modelPreferences +
					", systemPrompt='" + systemPrompt + '\'' +
					", includeContext=" + includeContext +
					", temperature=" + temperature +
					", maxTokens=" + maxTokens +
					", stopSequences=" + stopSequences +
					", metadata=" + metadata +
					'}';
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

		public CreateMessageRequest() {
		}

		public CreateMessageRequest(List<SamplingMessage> messages, ModelPreferences modelPreferences, String systemPrompt, ContextInclusionStrategy includeContext, Double temperature, int maxTokens, List<String> stopSequences, Map<String, Object> metadata) {
			this.messages = messages;
			this.modelPreferences = modelPreferences;
			this.systemPrompt = systemPrompt;
			this.includeContext = includeContext;
			this.temperature = temperature;
			this.maxTokens = maxTokens;
			this.stopSequences = stopSequences;
			this.metadata = metadata;
		}

		public enum ContextInclusionStrategy {
			@JsonProperty("none") NONE,
			@JsonProperty("thisServer") THIS_SERVER,
			@JsonProperty("allServers") ALL_SERVERS
		}
		
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

			public CreateMessageRequest build() {
				return new CreateMessageRequest(messages, modelPreferences, systemPrompt,
					includeContext, temperature, maxTokens, stopSequences, metadata);
			}
		}
	}// @formatter:on

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CreateMessageResult {
		@JsonProperty("role") Role role;
		@JsonProperty("content") Content content;
		@JsonProperty("model") String model;
		@JsonProperty("stopReason") StopReason stopReason;

		@Override
		public String toString() {
			return "CreateMessageResult{" +
					"role=" + role +
					", content=" + content +
					", model='" + model + '\'' +
					", stopReason=" + stopReason +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			CreateMessageResult that = (CreateMessageResult) o;
			return role == that.role && Objects.equals(content, that.content) && Objects.equals(model, that.model) && stopReason == that.stopReason;
		}

		@Override
		public int hashCode() {
			return Objects.hash(role, content, model, stopReason);
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

		public CreateMessageResult() {
		}

		public CreateMessageResult(Role role, Content content, String model, StopReason stopReason) {
			this.role = role;
			this.content = content;
			this.model = model;
			this.stopReason = stopReason;
		}

		public enum StopReason {
			@JsonProperty("endTurn") END_TURN,
			@JsonProperty("stopSequence") STOP_SEQUENCE,
			@JsonProperty("maxTokens") MAX_TOKENS
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private Role role = Role.ASSISTANT;
			private Content content;
			private String model;
			private StopReason stopReason = StopReason.END_TURN;

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

			public CreateMessageResult build() {
				return new CreateMessageResult(role, content, model, stopReason);
			}
		}
	}// @formatter:on

	// ---------------------------
	// Pagination Interfaces
	// ---------------------------
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PaginatedRequest {
		@JsonProperty("cursor") String cursor;

		@Override
		public String toString() {
			return "PaginatedRequest{" +
					"cursor='" + cursor + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			PaginatedRequest that = (PaginatedRequest) o;
			return Objects.equals(cursor, that.cursor);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(cursor);
		}

		public String getCursor() {
			return cursor;
		}

		public void setCursor(String cursor) {
			this.cursor = cursor;
		}

		public PaginatedRequest() {
		}

		public PaginatedRequest(String cursor) {
			this.cursor = cursor;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PaginatedResult {
		@JsonProperty("nextCursor") String nextCursor;

		@Override
		public String toString() {
			return "PaginatedResult{" +
					"nextCursor='" + nextCursor + '\'' +
					'}';
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

		public PaginatedResult() {
		}

		public PaginatedResult(String nextCursor) {
			this.nextCursor = nextCursor;
		}
	}

	// ---------------------------
	// Progress and Logging
	// ---------------------------
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProgressNotification {
		@JsonProperty("progressToken") String progressToken;
		@JsonProperty("progress") double progress;
		@JsonProperty("total") Double total;

		@Override
		public String toString() {
			return "ProgressNotification{" +
					"progressToken='" + progressToken + '\'' +
					", progress=" + progress +
					", total=" + total +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ProgressNotification that = (ProgressNotification) o;
			return Double.compare(progress, that.progress) == 0 && Objects.equals(progressToken, that.progressToken) && Objects.equals(total, that.total);
		}

		@Override
		public int hashCode() {
			return Objects.hash(progressToken, progress, total);
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

		public ProgressNotification() {
		}

		public ProgressNotification(String progressToken, double progress, Double total) {
			this.progressToken = progressToken;
			this.progress = progress;
			this.total = total;
		}
	}// @formatter:on

	/**
	 * The Model Context Protocol (MCP) provides a standardized way for servers to send
	 * structured log messages to clients. Clients can control logging verbosity by
	 * setting minimum log levels, with servers sending notifications containing severity
	 * levels, optional logger names, and arbitrary JSON-serializable data.
	 *
	 * level The severity levels. The mimimum log level is set by the client.
	 * logger The logger that generated the message.
	 * data JSON-serializable logging data.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LoggingMessageNotification {
		@JsonProperty("level") LoggingLevel level;
		@JsonProperty("logger") String logger;
		@JsonProperty("data") String data;

		@Override
		public String toString() {
			return "LoggingMessageNotification{" +
					"level=" + level +
					", logger='" + logger + '\'' +
					", data='" + data + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			LoggingMessageNotification that = (LoggingMessageNotification) o;
			return level == that.level && Objects.equals(logger, that.logger) && Objects.equals(data, that.data);
		}

		@Override
		public int hashCode() {
			return Objects.hash(level, logger, data);
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

		public LoggingMessageNotification() {
		}

		public LoggingMessageNotification(LoggingLevel level, String logger, String data) {
			this.level = level;
			this.logger = logger;
			this.data = data;
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private LoggingLevel level = LoggingLevel.INFO;
			private String logger = "server";
			private String data;

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

			public LoggingMessageNotification build() {
				return new LoggingMessageNotification(level, logger, data);
			}
		}
	}// @formatter:on

	public enum LoggingLevel {// @formatter:off
		@JsonProperty("debug") DEBUG(0),
		@JsonProperty("info") INFO(1),
		@JsonProperty("notice") NOTICE(2),
		@JsonProperty("warning") WARNING(3),
		@JsonProperty("error") ERROR(4),
		@JsonProperty("critical") CRITICAL(5),
		@JsonProperty("alert") ALERT(6),
		@JsonProperty("emergency") EMERGENCY(7);

		private final int level;

		LoggingLevel(int level) {
			this.level = level;
		}

		public int level() {
			return level;
		}

	} // @formatter:on

	// ---------------------------
	// Autocomplete
	// ---------------------------
	public static class CompleteRequest implements Request {
		PromptOrResourceReference ref;
		CompleteArgument argument;

		@Override
		public String toString() {
			return "CompleteRequest{" +
					"ref=" + ref +
					", argument=" + argument +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			CompleteRequest that = (CompleteRequest) o;
			return Objects.equals(ref, that.ref) && Objects.equals(argument, that.argument);
		}

		@Override
		public int hashCode() {
			return Objects.hash(ref, argument);
		}

		public PromptOrResourceReference getRef() {
			return ref;
		}

		public void setRef(PromptOrResourceReference ref) {
			this.ref = ref;
		}

		public CompleteArgument getArgument() {
			return argument;
		}

		public void setArgument(CompleteArgument argument) {
			this.argument = argument;
		}

		public CompleteRequest() {
		}

		public CompleteRequest(PromptOrResourceReference ref, CompleteArgument argument) {
			this.ref = ref;
			this.argument = argument;
		}

		public interface PromptOrResourceReference {
			String getType();
		}


		public static class PromptReference implements PromptOrResourceReference {
			@JsonProperty("type") String type;
			@JsonProperty("name") String name;

			@Override
			public String toString() {
				return "PromptReference{" +
						"type='" + type + '\'' +
						", name='" + name + '\'' +
						'}';
			}

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass()) return false;
				PromptReference that = (PromptReference) o;
				return Objects.equals(type, that.type) && Objects.equals(name, that.name);
			}

			@Override
			public int hashCode() {
				return Objects.hash(type, name);
			}

			@Override
			public String getType() {
				return type;
			}

			public void setType(String type) {
				this.type = type;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public PromptReference() {
			}

			public PromptReference(String type, String name) {
				this.type = type;
				this.name = name;
			}
		}// @formatter:on


		public static class ResourceReference implements PromptOrResourceReference {
			@JsonProperty("type") String type;
			@JsonProperty("uri") String uri;

			@Override
			public String toString() {
				return "ResourceReference{" +
						"type='" + type + '\'' +
						", uri='" + uri + '\'' +
						'}';
			}

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass()) return false;
				ResourceReference that = (ResourceReference) o;
				return Objects.equals(type, that.type) && Objects.equals(uri, that.uri);
			}

			@Override
			public int hashCode() {
				return Objects.hash(type, uri);
			}

			@Override
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

			public ResourceReference() {
			}

			public ResourceReference(String type, String uri) {
				this.type = type;
				this.uri = uri;
			}
		}// @formatter:on

		public static class CompleteArgument {
			@JsonProperty("name") String name;
			@JsonProperty("value") String value;

			@Override
			public String toString() {
				return "CompleteArgument{" +
						"name='" + name + '\'' +
						", value='" + value + '\'' +
						'}';
			}

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass()) return false;
				CompleteArgument that = (CompleteArgument) o;
				return Objects.equals(name, that.name) && Objects.equals(value, that.value);
			}

			@Override
			public int hashCode() {
				return Objects.hash(name, value);
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getValue() {
				return value;
			}

			public void setValue(String value) {
				this.value = value;
			}

			public CompleteArgument() {
			}

			public CompleteArgument(String name, String value) {
				this.name = name;
				this.value = value;
			}
		}// @formatter:on
	}

	public static class CompleteResult {
		CompleteCompletion completion;

		@Override
		public String toString() {
			return "CompleteResult{" +
					"completion=" + completion +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			CompleteResult that = (CompleteResult) o;
			return Objects.equals(completion, that.completion);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(completion);
		}

		public CompleteCompletion getCompletion() {
			return completion;
		}

		public void setCompletion(CompleteCompletion completion) {
			this.completion = completion;
		}

		public CompleteResult() {
		}

		public CompleteResult(CompleteCompletion completion) {
			this.completion = completion;
		}

		public static class CompleteCompletion {
			@JsonProperty("values") List<String> values;
			@JsonProperty("total") Integer total;
			@JsonProperty("hasMore") Boolean hasMore;

			@Override
			public String toString() {
				return "CompleteCompletion{" +
						"values=" + values +
						", total=" + total +
						", hasMore=" + hasMore +
						'}';
			}

			@Override
			public boolean equals(Object o) {
				if (o == null || getClass() != o.getClass()) return false;
				CompleteCompletion that = (CompleteCompletion) o;
				return Objects.equals(values, that.values) && Objects.equals(total, that.total) && Objects.equals(hasMore, that.hasMore);
			}

			@Override
			public int hashCode() {
				return Objects.hash(values, total, hasMore);
			}

			public List<String> getValues() {
				return values;
			}

			public void setValues(List<String> values) {
				this.values = values;
			}

			public Integer getTotal() {
				return total;
			}

			public void setTotal(Integer total) {
				this.total = total;
			}

			public Boolean getHasMore() {
				return hasMore;
			}

			public void setHasMore(Boolean hasMore) {
				this.hasMore = hasMore;
			}

			public CompleteCompletion() {
			}

			public CompleteCompletion(List<String> values, Integer total, Boolean hasMore) {
				this.values = values;
				this.total = total;
				this.hasMore = hasMore;
			}
		}// @formatter:on
	}

	// ---------------------------
	// Content Types
	// ---------------------------
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "type")
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextContent.class, name = "text"),
			@JsonSubTypes.Type(value = ImageContent.class, name = "image"),
			@JsonSubTypes.Type(value = EmbeddedResource.class, name = "resource") })
	public interface Content {

		default String type() {
			if (this instanceof TextContent) {
				return "text";
			}
			else if (this instanceof ImageContent) {
				return "image";
			}
			else if (this instanceof EmbeddedResource) {
				return "resource";
			}
			throw new IllegalArgumentException("Unknown content type: " + this);
		}

	}

	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TextContent implements Content { // @formatter:on
		@JsonProperty("audience") List<Role> audience;
		@JsonProperty("priority") Double priority;
		@JsonProperty("text") String text;

		@Override
		public String toString() {
			return "TextContent{" +
					"audience=" + audience +
					", priority=" + priority +
					", text='" + text + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			TextContent that = (TextContent) o;
			return Objects.equals(audience, that.audience) && Objects.equals(priority, that.priority) && Objects.equals(text, that.text);
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

		public TextContent() {
		}

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
		@JsonProperty("audience") List<Role> audience;
		@JsonProperty("priority") Double priority;
		@JsonProperty("data") String data;
		@JsonProperty("mimeType") String mimeType;

		@Override
		public String toString() {
			return "ImageContent{" +
					"audience=" + audience +
					", priority=" + priority +
					", data='" + data + '\'' +
					", mimeType='" + mimeType + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ImageContent that = (ImageContent) o;
			return Objects.equals(audience, that.audience) && Objects.equals(priority, that.priority) && Objects.equals(data, that.data) && Objects.equals(mimeType, that.mimeType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(audience, priority, data, mimeType);
		}

		public ImageContent() {
		}

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
		@JsonProperty("audience") List<Role> audience;
		@JsonProperty("priority") Double priority;
		@JsonProperty("resource") ResourceContents resource;

		@Override
		public String toString() {
			return "EmbeddedResource{" +
					"audience=" + audience +
					", priority=" + priority +
					", resource=" + resource +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			EmbeddedResource that = (EmbeddedResource) o;
			return Objects.equals(audience, that.audience) && Objects.equals(priority, that.priority) && Objects.equals(resource, that.resource);
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

		public EmbeddedResource() {
		}

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
	 *
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
		@JsonProperty("uri") String uri;
		@JsonProperty("name") String name;

		@Override
		public String toString() {
			return "Root{" +
					"uri='" + uri + '\'' +
					", name='" + name + '\'' +
					'}';
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

		public Root() {
		}

		public Root(String uri, String name) {
			this.uri = uri;
			this.name = name;
		}
	} // @formatter:on

	/**
	 * The client's response to a roots/list request from the server. This result contains
	 * an array of Root objects, each representing a root directory or file that the
	 * server can operate on.
	 *
	 * roots An array of Root objects, each representing a root directory or file
	 * that the server can operate on.
	 */
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListRootsResult {
		@JsonProperty("roots") List<Root> roots;

		@Override
		public String toString() {
			return "ListRootsResult{" +
					"roots=" + roots +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || getClass() != o.getClass()) return false;
			ListRootsResult that = (ListRootsResult) o;
			return Objects.equals(roots, that.roots);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(roots);
		}

		public List<Root> getRoots() {
			return roots;
		}

		public void setRoots(List<Root> roots) {
			this.roots = roots;
		}

		public ListRootsResult() {
		}

		public ListRootsResult(List<Root> roots) {
			this.roots = roots;
		}
	} // @formatter:on

}
