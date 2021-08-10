package io.seata.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigUrl {

    /** config url */
    private String url;
    /** config type */
    private String protocol;
    /** config serverAddr */
    private String host;
    private int port;
    private String path;
    /** config center paramters */
    private Map<String, String> parameters = new HashMap<>();


    private static ConfigUrl instance;

    public static ConfigUrl getInstance() {
        if (instance == null) {
            synchronized (ConfigUrl.class) {
                String url = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_CONFIG
                        + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.URL);
                instance = new ConfigUrl(url);
            }
        }
        return instance;
    }

    public ConfigUrl(String url) {
        valueOf(url);
    }

    public void valueOf(String url) {
        if (url != null && (url.trim().length() != 0)) {
            int index = url.indexOf(63);
            if (index > 0) {
                String[] params = url.substring(index + 1).split("&");
                for (String param : params) {
                    if (param.trim().length() > 0) {
                        int i = param.indexOf(61);
                        if (i >= 0) {
                            String key = param.substring(0, i);
                            String value = param.substring(i + 1);
                            parameters.put(key, value);
                        } else {
                            parameters.put(param, param);
                        }
                    }
                }
                url = url.substring(0, index);
            }
            index = url.indexOf("://");
            if (index >= 0) {
                if (index == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                }
                this.protocol = url.substring(0, index);
                url = url.substring(index + 3);
            }
            index = url.indexOf(47);
            if (index >= 0) {
                this.path = url.substring(index + 1);
                url = url.substring(0, index);
            }
            index = url.lastIndexOf(58);
            if (index >= 0 && index < url.length() - 1) {
                this.port = Integer.parseInt(url.substring(index + 1));
            }
            this.host = url;
        }else {
            throw new IllegalArgumentException("url == null");
        }
    }


    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getUsername() {
        return this.parameters.get("username");
    }

    public String getPassword() {
        return this.parameters.get("password");
    }
}