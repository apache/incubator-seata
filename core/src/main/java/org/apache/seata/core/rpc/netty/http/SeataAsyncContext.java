package org.apache.seata.core.rpc.netty.http;

import org.apache.seata.common.exception.SeataRuntimeException;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SeataAsyncContext implements AsyncContext {
    private SeataHttpServletRequest request;
    private SeataHttpServletResponse response;
    private List<AsyncListener> asyncListenerList;

    public SeataAsyncContext(SeataHttpServletRequest request, SeataHttpServletResponse response) {
        this.request = request;
        this.response = response;
        asyncListenerList = new ArrayList<>();
    }

    @Override
    public ServletRequest getRequest() {
        return request;
    }

    @Override
    public ServletResponse getResponse() {
        return response;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return false;
    }

    @Override
    public void dispatch() {

    }

    @Override
    public void dispatch(String path) {

    }

    @Override
    public void dispatch(ServletContext context, String path) {

    }

    @Override
    public void complete() {
        for (AsyncListener asyncListener : asyncListenerList) {
            try {
                asyncListener.onComplete(new AsyncEvent(this));
            } catch (IOException e) {
                throw new RuntimeException("SeataAsyncContext complete error: " + e.getMessage());
            }
        }
    }

    @Override
    public void start(Runnable run) {

    }

    @Override
    public void addListener(AsyncListener listener) {
        asyncListenerList.add(listener);
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {

    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public void setTimeout(long timeout) {

    }

    @Override
    public long getTimeout() {
        return 0;
    }
}
