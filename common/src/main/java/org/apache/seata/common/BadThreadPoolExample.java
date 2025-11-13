package org.apache.seata.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BadThreadPoolExample {
    public void startTask() {
        // 故意违反PMD的ThreadPoolCreationRule：使用Executors创建线程池（不推荐）
        // PMD规则要求：手动创建ThreadPoolExecutor并指定核心参数（如核心线程数、拒绝策略等）
        ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.submit(() -> System.out.println("执行任务"));
    }
}
