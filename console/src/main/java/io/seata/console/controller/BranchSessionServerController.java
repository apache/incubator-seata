package io.seata.console.controller;

import io.seata.console.config.WebSecurityConfig;
import io.seata.console.result.BranchSessionVO;
import io.seata.console.result.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping("/console/branchSession")
public class BranchSessionServerController {
    @Autowired
    RestTemplate restTemplate;
    @Resource
    HttpServletRequest request;

    @Value("${console.user.password}")
    private  String pass;

    @Value("${seata.server.address}")
    private String address;

    @Value("${seata.server.port}")
    private String port;

    private static final Logger LOGGER = LoggerFactory.getLogger(BranchSessionServerController.class);


    PageResult<BranchSessionVO> queryByXid(String xid) {
        String requesturl = request.getRequestURI();
        String queryString = request.getQueryString();
        // innner http request url
        String url = "http://" + address + ":" + port + "/server" + requesturl + "?" + queryString;
        // get bearerToken
        String bearerToken = request.getHeader(WebSecurityConfig.AUTHORIZATION_HEADER);
        PageResult response = new PageResult();
        // add beartoken header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<String> httpEntity = new HttpEntity(null, httpHeaders);
        try {
            // innner http request
            ResponseEntity<PageResult> result = restTemplate.exchange(url, HttpMethod.GET, httpEntity, PageResult.class);
            response = result.getBody();
        } catch (Exception e) {
            LOGGER.error("Server request error：", e);
        }
        return response;
    }
}
