package io.seata.spring.schema;

import java.util.Objects;

/**
 * The type gtx target
 *
 * @author xingfudeshi@gmail.com
 */
public class GtxTarget {
    private GtxTargetType gtxTargetType;
    private String targetName;
    private GtxConfig gtxConfig;

    public GtxTargetType getGtxTargetType() {
        return gtxTargetType;
    }

    public void setGtxTargetType(GtxTargetType gtxTargetType) {
        this.gtxTargetType = gtxTargetType;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public GtxConfig getGtxConfig() {
        return gtxConfig;
    }

    public void setGtxConfig(GtxConfig gtxConfig) {
        this.gtxConfig = gtxConfig;
    }

    @Override
    public String toString() {
        return "GtxTarget{" +
            "gtxTargetType=" + gtxTargetType +
            ", targetName='" + targetName + '\'' +
            ", gtxConfig=" + gtxConfig +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GtxTarget that = (GtxTarget) o;
        return gtxTargetType == that.gtxTargetType && Objects.equals(targetName, that.targetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gtxTargetType, targetName);
    }
}
