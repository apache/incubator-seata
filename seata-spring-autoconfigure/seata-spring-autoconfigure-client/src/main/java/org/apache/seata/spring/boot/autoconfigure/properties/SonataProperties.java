package org.apache.seata.spring.boot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SONATA_PREFIX;

@Component
@ConfigurationProperties(prefix = SONATA_PREFIX)
public class SonataProperties {
    private boolean enableGlobalSerializability;
    private String dummyTable;
    private int dummyTableSize;
    private int s2plDummyWriteRetryWarningThreshold;
    private int ssiHelperBatchSize;

    public boolean isEnableGlobalSerializability() {
        return enableGlobalSerializability;
    }

    public SonataProperties setEnableGlobalSerializability(boolean enableGlobalSerializability) {
        this.enableGlobalSerializability = enableGlobalSerializability;
        return this;
    }

    public String getDummyTable() {
        return dummyTable;
    }

    public SonataProperties setDummyTable(String dummyTable) {
        this.dummyTable = dummyTable;
        return this;
    }

    public int getDummyTableSize() {
        return dummyTableSize;
    }

    public SonataProperties setDummyTableSize(int dummyTableSize) {
        this.dummyTableSize = dummyTableSize;
        return this;
    }

    public int getS2plDummyWriteRetryWarningThreshold() {
        return s2plDummyWriteRetryWarningThreshold;
    }

    public SonataProperties setS2plDummyWriteRetryWarningThreshold(int s2plDummyWriteRetryWarningThreshold) {
        this.s2plDummyWriteRetryWarningThreshold = s2plDummyWriteRetryWarningThreshold;
        return this;
    }

    public int getSsiHelperBatchSize() {
        return ssiHelperBatchSize;
    }

    public SonataProperties setSsiHelperBatchSize(int ssiHelperBatchSize) {
        this.ssiHelperBatchSize = ssiHelperBatchSize;
        return this;
    }
}
