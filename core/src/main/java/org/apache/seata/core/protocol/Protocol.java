package org.apache.seata.core.protocol;

/**
 *
 */
public enum Protocol {
    GPRC("gprc"),

    SEATA("seata");

    public final String value;

    Protocol(String value) {
        this.value = value;
    }
}
