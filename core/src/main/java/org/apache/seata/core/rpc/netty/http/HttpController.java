package org.apache.seata.core.rpc.netty.http;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface HttpController {
    Set<String> getPath();

    String handle(String path, Map<String, List<String>> paramMap);
}
