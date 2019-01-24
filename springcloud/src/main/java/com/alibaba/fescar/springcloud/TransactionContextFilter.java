package com.alibaba.fescar.springcloud;

import com.alibaba.fescar.core.context.RootContext;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class TransactionContextFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionContextFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        FilterChain filterChain) throws ServletException, IOException {
        String xid = httpServletRequest.getHeader(RootContext.KEY_XID);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in request[" + xid + "]");
        }
        boolean bind = false;
        if (xid != null) {
            RootContext.bind(xid);
            bind = true;
        }
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            if (bind) {
                RootContext.unbind();
            }
        }
    }
}
