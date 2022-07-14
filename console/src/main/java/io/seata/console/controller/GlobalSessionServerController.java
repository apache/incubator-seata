package io.seata.console.controller;

import io.seata.console.config.WebSecurityConfig;
import io.seata.console.result.GlobalSessionVO;
import io.seata.console.result.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author: Sher
 * @date: 2022/7/12
 */


@RestController
@RequestMapping("api/v1/console/globalSession")
public class GlobalSessionServerController {
    @Autowired
    RestTemplate restTemplate;
    @Resource
    HttpServletRequest request;
//    @Value("${seata.server.port}")

    @Value("${seata.server.port}")
    private Integer port;
//    private Integer port;

    @Value("${seata.server.address}")
    private String address;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionServerController.class);


    @RequestMapping("query")
    PageResult<GlobalSessionVO> queryByXid() {
        String requesturl = request.getRequestURI();
        String queryString = request.getQueryString();
        String url = "http://" + address + ":" + port + "/server" + requesturl + "?" + queryString;
        // get bearerToken
        String bearerToken = request.getHeader(WebSecurityConfig.AUTHORIZATION_HEADER);
        PageResult response = new PageResult();
        HttpHeaders httpHeaders = new HttpHeaders();
        //add bearertoken header
        httpHeaders.add(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<String> httpEntity = new HttpEntity(null, httpHeaders);
        try {
            ResponseEntity<PageResult> result = restTemplate.exchange(url, HttpMethod.GET, httpEntity, PageResult.class);
            response = result.getBody();
        } catch (Exception e) {
            LOGGER.error("Server request error：", e);
        }

        return response;
    }
}
