package com.alibaba.fescar.rm.datasource.plugin;

import java.util.List;

public interface Plugin {

    /**
     * 当前插件支持的action列表
     *
     * @return
     */
    List<String> supportedActions();

    /**
     * 执行业务处理,并返回结果对象
     *
     * @param context
     * @return
     */
    void proc(PluginContext context);
}
