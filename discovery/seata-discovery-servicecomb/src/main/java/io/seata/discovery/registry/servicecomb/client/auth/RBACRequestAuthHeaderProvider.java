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

package io.seata.discovery.registry.servicecomb.client.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.servicecomb.SeataServicecombKeys;
import io.seata.config.servicecomb.client.EventManager;
import io.seata.config.servicecomb.client.auth.AuthHeaderProviders;

import org.apache.commons.lang3.StringUtils;
import org.apache.servicecomb.foundation.auth.AuthHeaderProvider;
import org.apache.servicecomb.http.client.common.HttpConfiguration;
import org.apache.servicecomb.service.center.client.AddressManager;
import org.apache.servicecomb.service.center.client.OperationEvents;
import org.apache.servicecomb.service.center.client.ServiceCenterClient;
import org.apache.servicecomb.service.center.client.model.RbacTokenRequest;
import org.apache.servicecomb.service.center.client.model.RbacTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response.Status;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RBACRequestAuthHeaderProvider implements AuthHeaderProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(RBACRequestAuthHeaderProvider.class);

  // special token used for special conditions
  // e.g. un-authorized: will query token after token expired period
  // e.g. not found:  will query token after token expired period
  public static final String INVALID_TOKEN = "invalid";

  private static final String UN_AUTHORIZED_CODE_HALF_OPEN = "401302";

  public static final String CACHE_KEY = "token";

  public static final String AUTH_HEADER = "Authorization";

  private static final long TOKEN_REFRESH_TIME_IN_SECONDS = 20 * 60 * 1000;

  private ExecutorService executorService;

  private LoadingCache<String, String> cache;

  private String lastErrorCode = "401302";

  private int lastStatusCode = 401;

  private ServiceCenterClient client;

  private Configuration properties = ConfigurationFactory.CURRENT_FILE_INSTANCE;

  public RBACRequestAuthHeaderProvider() {
    EventManager.register(this);

    if (enabled()) {
      client = createServiceCenterClient();
      executorService = new ThreadPoolExecutor(1, 1,
              0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>(),
              t -> new Thread(t, "rbac-executor"));
      ListeningExecutorService service = MoreExecutors.listeningDecorator(executorService);
      cache = CacheBuilder.newBuilder()
          .maximumSize(1)
          .refreshAfterWrite(refreshTime(), TimeUnit.MILLISECONDS)
          .build(new CacheLoader<String, String>() {
            @Override
            public String load(String key) {
              return createHeaders();
            }

            @Override
            public ListenableFuture<String> reload(String key, String oldValue) {
              return Futures.submitAsync(Callables.asAsyncCallable(() -> createHeaders(), service), executorService);
            }
          });
    }
  }


  private ServiceCenterClient createServiceCenterClient() {
    AddressManager addressManager = createAddressManager();
    HttpConfiguration.SSLProperties sslProperties =
        AuthHeaderProviders.createSslProperties(properties);
    return new ServiceCenterClient(addressManager, sslProperties, signRequest -> Collections.emptyMap()
        , SeataServicecombKeys.DEFAULT, null);
  }

  private AddressManager createAddressManager() {
    String address =
        properties.getConfig(SeataServicecombKeys.KEY_REGISTRY_ADDRESS, SeataServicecombKeys.DEFAULT_REGISTRY_URL);
    String project = properties.getConfig(SeataServicecombKeys.KEY_SERVICE_PROJECT, SeataServicecombKeys.DEFAULT);
    LOGGER.info("Using service center, address={}.", address);
    return new AddressManager(project, Arrays.asList(address.split(SeataServicecombKeys.COMMA)));
  }

  @Subscribe
  public void onNotPermittedEvent(OperationEvents.UnAuthorizedOperationEvent event) {
    this.executorService.submit(this::retryRefresh);
  }

  protected String createHeaders() {
    LOGGER.info("start to create RBAC headers");

    RbacTokenResponse rbacTokenResponse = callCreateHeaders();
    lastErrorCode = rbacTokenResponse.getErrorCode();
    lastStatusCode = rbacTokenResponse.getStatusCode();

    if (Status.UNAUTHORIZED.getStatusCode() == rbacTokenResponse.getStatusCode()
        || Status.FORBIDDEN.getStatusCode() == rbacTokenResponse.getStatusCode()) {
      // password wrong, do not try anymore
      LOGGER.warn("username or password may be wrong, stop trying to query tokens.");
      return INVALID_TOKEN;
    } else if (Status.NOT_FOUND.getStatusCode() == rbacTokenResponse.getStatusCode()) {
      // service center not support, do not try
      LOGGER.warn("service center do not support RBAC token, you should not config account info");
      return INVALID_TOKEN;
    }

    LOGGER.info("refresh token successfully {}", rbacTokenResponse.getStatusCode());
    return rbacTokenResponse.getToken();
  }

  protected RbacTokenResponse callCreateHeaders() {
    RbacTokenRequest request = new RbacTokenRequest();
    request.setName(properties.getConfig(SeataServicecombKeys.KEY_RBAC_NAME));
    request.setPassword(properties.getConfig(SeataServicecombKeys.KEY_RBAC_PASSWORD));

    return client.queryToken(request);
  }

  protected long refreshTime() {
    return TOKEN_REFRESH_TIME_IN_SECONDS;
  }

  @Override
  public Map<String, String> authHeaders() {
    if (!enabled()) {
      return Collections.emptyMap();
    }

    try {
      String header = cache.get(CACHE_KEY);
      if (!StringUtils.isEmpty(header)) {
        Map<String, String> tokens = new HashMap<>(1);
        tokens.put(AUTH_HEADER, "Bearer " + header);
        return tokens;
      }
    } catch (Exception e) {
      LOGGER.error("Get auth headers failed", e);
    }
    return Collections.emptyMap();
  }

  private boolean enabled() {
    return !StringUtils.isEmpty(properties.getConfig(SeataServicecombKeys.KEY_RBAC_NAME)) && !StringUtils
        .isEmpty(properties.getConfig(SeataServicecombKeys.KEY_RBAC_PASSWORD));
  }

  private void retryRefresh() {
    if (Status.UNAUTHORIZED.getStatusCode() == lastStatusCode && UN_AUTHORIZED_CODE_HALF_OPEN.equals(lastErrorCode)) {
      cache.refresh(properties.getConfig(SeataServicecombKeys.KEY_SERVICE_NAME));
    }
  }
}
