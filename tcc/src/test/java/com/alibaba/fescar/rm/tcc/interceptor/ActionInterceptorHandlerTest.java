package com.alibaba.fescar.rm.tcc.interceptor;

import com.alibaba.fescar.rm.tcc.TccAction;
import com.alibaba.fescar.rm.tcc.TccParam;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhangsen
 */
public class ActionInterceptorHandlerTest {

    protected  ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    @Test
    public void testBusinessActionContext() throws NoSuchMethodException {
        Method prepareMethod = TccAction.class.getDeclaredMethod("prepare",
                new Class[]{BusinessActionContext.class, int.class, List.class, TccParam.class});
        List list = new ArrayList();
        list.add("b");
        TccParam tccParam = new TccParam (1, "abc@ali.com");

        Map<String, Object>  paramContext = actionInterceptorHandler.fetchActionRequestContext(prepareMethod,
                new Object[]{null, 10, list, tccParam});
        System.out.println(paramContext);

        Assert.assertEquals(10, paramContext.get("a"));
        Assert.assertEquals("b", paramContext.get("b"));
        Assert.assertEquals("abc@ali.com", paramContext.get("email"));
    }

}