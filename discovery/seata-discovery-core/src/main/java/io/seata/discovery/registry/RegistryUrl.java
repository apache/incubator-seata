package io.seata.discovery.registry;


import java.util.HashMap;
import java.util.Map;

public class RegistryUrl {

    private final String host;

    private final int port;

    private final String schema;

    private final HashMap<String,String> parameters;

    public RegistryUrl() {
        this(null, null, 0, null);
    }

    public RegistryUrl(String host,int port){
        this(null,host, port, null);
    }

    public RegistryUrl(String schema,String host, int port, Map<String,String> parameters){
        this.schema = schema;
        this.host = host;
        this.port = port;
        if (parameters == null) {
            this.parameters = new HashMap<>();
        }else{
            this.parameters = new HashMap<>(parameters);
        }
    }

    public static RegistryUrl valueOf(String url) {
        if (url == null || url.trim().length() == 0) {
            throw new IllegalArgumentException("url is null or empty string");
        }
        int port;
        String host;
        String schema;
        String param;
        int idxOfSchema = url.indexOf("://");
        int idxOfPort;
        int idxOfPara = url.indexOf("?");
        int idxOfHost;

        if (idxOfSchema < 0) {
            schema = null;
            idxOfHost = 0;
            idxOfPort = url.indexOf(":");
        }else{
            schema = url.substring(0, idxOfSchema);
            idxOfHost = idxOfSchema + 3;
            idxOfPort = url.indexOf(":", idxOfSchema + 3);
        }
        if (idxOfPara > 0) {
            param = url.substring(idxOfPara + 1);
        }
        if (idxOfPort < 0) {
            port = 0;
            if (idxOfPara < 0) {
                host = url.substring(idxOfHost);
            }else{
                host = url.substring(idxOfHost, idxOfPara);
            }
        } else if (idxOfPort > 0 && idxOfPara > 0) {
            port = Integer.valueOf(url.substring(idxOfPort + 1, idxOfPara));
            host = url.substring(idxOfHost, idxOfPort);
        }else {
            port = Integer.valueOf(url.substring(idxOfPort + 1));
            host = url.substring(idxOfHost, idxOfPort);
        }
        return new RegistryUrl(schema, host, port, null);
    }

    public static Map<String, String> parseParamter(String paraStr) {

        HashMap<String, String> parameters = new HashMap<>();

        int idxOfSeg = 0;

        do {
            int idxOfeql = paraStr.indexOf("=",idxOfSeg + 1);

            if (idxOfeql < 0) {
                idxOfSeg = paraStr.indexOf("&", idxOfSeg + 1);
                break;
            }else{
                String val;

                String key = paraStr.substring(idxOfSeg == 0 ? 0 : idxOfSeg + 1, idxOfeql).trim();
                idxOfSeg = paraStr.indexOf("&", idxOfSeg + 1);
                if (idxOfSeg > 0) {
                    val = paraStr.substring(idxOfeql + 1,idxOfSeg).trim();
                    parameters.put(key, val);
                }else{
                    val = paraStr.substring(idxOfeql + 1).trim();
                    parameters.put(key, val);
                    return parameters;
                }
            }
        } while (idxOfSeg > 0);

        return parameters;
    }


    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getSchema() {
        return schema;
    }


    @Override
    public String toString() {
        return "RegistryUrl{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", schema='" + schema + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
