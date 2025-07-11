package org.apache.seata.namingserver.filter;

import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.namingserver.entity.vo.NamespaceVO;
import org.apache.seata.namingserver.manager.NamingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.seata.common.Constants.RAFT_GROUP_HEADER;
import static org.apache.seata.namingserver.contants.NamingConstant.MCP_PATTERN;


public class MCPRemotingFilter implements Filter {

    private final NamingManager namingManager;

    private final AsyncRestTemplate asyncRestTemplate;

    private final Pattern urlPattern = Pattern.compile(MCP_PATTERN);

    private final Logger logger = LoggerFactory.getLogger(MCPRemotingFilter.class);

    public MCPRemotingFilter(NamingManager namingManager, AsyncRestTemplate asyncRestTemplate) {
        this.namingManager = namingManager;
        this.asyncRestTemplate = asyncRestTemplate;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            if (urlPattern
                    .matcher(((HttpServletRequest) servletRequest).getRequestURI())
                    .matches()) {
                CachedBodyHttpServletRequest request =
                        new CachedBodyHttpServletRequest((HttpServletRequest) servletRequest);
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                SingleResult<Map<String, NamespaceVO>> namespaces = namingManager.namespace();
                String namespace = "";
                String cluster = "";
                if (namespaces != null && !namespaces.getData().isEmpty()) {
                    // 1. Collect all available namespaces
                    List<String> availableNamespaces = new ArrayList<>();
                    Map<String, List<String>> namespaceClusters = new HashMap<>();

                    for (Map.Entry<String, NamespaceVO> entry : namespaces.getData().entrySet()) {
                        String namespaceName = entry.getKey();
                        NamespaceVO namespaceVO = entry.getValue();

                        if (namespaceVO != null && namespaceVO.getClusters() != null) {
                            // Collect all non-empty clusters under the namespace
                            List<String> validClusters = namespaceVO.getClusters().stream()
                                    .filter(StringUtils::isNotBlank)
                                    .collect(Collectors.toList());

                            if (!validClusters.isEmpty()) {
                                availableNamespaces.add(namespaceName);
                                namespaceClusters.put(namespaceName, validClusters);
                            }
                        }
                    }

                    // 2. A namespace is selected at random
                    if (!availableNamespaces.isEmpty()) {
                        // Use a random index to select a namespace
                        int namespaceIndex = ThreadLocalRandom.current().nextInt(availableNamespaces.size());
                        namespace = availableNamespaces.get(namespaceIndex);

                        // 3. Randomly select the cluster under the namespace
                        List<String> clusters = namespaceClusters.get(namespace);
                        if (clusters != null && !clusters.isEmpty()) {
                            int clusterIndex = ThreadLocalRandom.current().nextInt(clusters.size());
                            cluster = clusters.get(clusterIndex);
                        }
                    }
                }
                if (StringUtils.isNotBlank(namespace)&&
                        StringUtils.isNotBlank(cluster)) {
                    List<Node> list = null;
                    if (StringUtils.isNotBlank(cluster)) {
                        list = namingManager.getInstances(namespace, cluster);
                    }
                    if (CollectionUtils.isNotEmpty(list)) {
                        // Randomly select a node from the list
                        NamingServerNode node = (NamingServerNode)
                                list.get(ThreadLocalRandom.current().nextInt(list.size()));
                        Node.Endpoint controlEndpoint = node.getControl();
                        if (controlEndpoint != null) {
                            // Construct the target URL
                            String targetUrl = "http://" + controlEndpoint.getHost() + ":" + controlEndpoint.getPort()
                                    + request.getRequestURI().replace("/mcp","")
                                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

                            // Copy headers from the original request
                            HttpHeaders headers = new HttpHeaders();
                            if (node.getRole() == ClusterRole.LEADER) {
                                headers.add(RAFT_GROUP_HEADER, node.getUnit());
                            }
                            Collections.list(request.getHeaderNames())
                                    .forEach(headerName -> headers.add(headerName, request.getHeader(headerName)));

                            // Create the HttpEntity with headers and body
                            HttpEntity<byte[]> httpEntity = new HttpEntity<>(request.getCachedBody(), headers);

                            // Forward the request
                            AsyncContext asyncContext = servletRequest.startAsync();
                            asyncContext.setTimeout(5000L);
                            ListenableFuture<ResponseEntity<byte[]>> responseEntityFuture = asyncRestTemplate.exchange(
                                    URI.create(targetUrl),
                                    Objects.requireNonNull(HttpMethod.resolve(request.getMethod())),
                                    httpEntity,
                                    byte[].class);
                            responseEntityFuture.addCallback(new ListenableFutureCallback<ResponseEntity<byte[]>>() {
                                @Override
                                public void onFailure(Throwable ex) {
                                    try {
                                        logger.error("Request to TC failed: {}", ex.getMessage());

                                        // Check whether it is an HTTP error response
                                        if (ex instanceof HttpStatusCodeException) {
                                            HttpStatusCodeException httpEx = (HttpStatusCodeException) ex;

                                            response.setStatus(httpEx.getRawStatusCode());

                                            if (httpEx.getResponseHeaders() != null) {
                                                httpEx.getResponseHeaders().forEach((key, values) -> {
                                                    values.forEach(value -> response.addHeader(key, value));
                                                });
                                            }

                                            byte[] responseBody = httpEx.getResponseBodyAsByteArray();
                                            if (responseBody.length > 0) {
                                                try (ServletOutputStream outputStream = response.getOutputStream()) {
                                                    outputStream.write(responseBody);
                                                    outputStream.flush();
                                                } catch (IOException e) {
                                                    logger.error("Error writing response body: {}", e.getMessage());
                                                }
                                            }
                                        } else {
                                            // Non-HTTP error, 500 is returned
                                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                            try (ServletOutputStream os = response.getOutputStream()) {
                                                os.write(("Error: " + ex.getMessage()).getBytes());
                                            } catch (IOException e) {
                                                logger.error("Error writing error response: {}", e.getMessage());
                                            }
                                        }
                                    } finally {
                                        asyncContext.complete();
                                    }
                                }

                                @Override
                                public void onSuccess(ResponseEntity<byte[]> responseEntity) {
                                    // Copy response headers and status code
                                    responseEntity.getHeaders().forEach((key, value) -> {
                                        value.forEach(v -> response.addHeader(key, v));
                                    });
                                    response.setStatus(responseEntity.getStatusCodeValue());
                                    // Write response body
                                    Optional.ofNullable(responseEntity.getBody())
                                            .ifPresent(body -> {
                                                try (ServletOutputStream outputStream = response.getOutputStream()) {
                                                    outputStream.write(body);
                                                    outputStream.flush();
                                                } catch (IOException e) {
                                                    logger.error(e.getMessage(), e);
                                                }
                                            });
                                    asyncContext.complete();
                                }
                            });
                            return;
                        }
                    }
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
