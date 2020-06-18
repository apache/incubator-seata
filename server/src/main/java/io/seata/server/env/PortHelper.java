package io.seata.server.env;

import io.seata.common.util.NumberUtils;

import static io.seata.server.ParameterParser.SERVER_DEFAULT_PORT;

/**
 * @author wang.liang
 */
public class PortHelper {

    public static int getPort(String[] args) {
        if (ContainerHelper.isRunningInContainer()) {
            return ContainerHelper.getPort();
        } else if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length; ++i) {
                if ("-p".equalsIgnoreCase(args[i]) && i < args.length - 1) {
                    return NumberUtils.toInt(args[i + 1], SERVER_DEFAULT_PORT);
                }
            }
        }

        return SERVER_DEFAULT_PORT;
    }

}
