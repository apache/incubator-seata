package com.alibaba.fescar.example.interceptor;

import com.alibaba.fescar.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TxMvcInterceptor implements HandlerInterceptor {

    static final Logger LOGGER = LoggerFactory.getLogger(TxMvcInterceptor.class);

    private static final String HEADER_TRANSACTION = "x-fescar-xid";


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        String xid = request.getHeader(HEADER_TRANSACTION);
        if (StringUtils.hasText(xid)) {
            RootContext.bind(xid);
            LOGGER.debug("bind xid. xid={}, uri={}", request.getRequestURI(), xid);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}