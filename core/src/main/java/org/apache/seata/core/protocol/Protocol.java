package org.apache.seata.core.protocol;

/**
 *
 */
public enum Protocol {

    GPRC("grpc"),

    SEATA("seata");

    public final String value;

    Protocol(String value) {
        this.value = value;
    }
}
