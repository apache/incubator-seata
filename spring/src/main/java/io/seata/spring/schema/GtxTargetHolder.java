package io.seata.spring.schema;

import java.lang.reflect.Method;
import java.util.Set;

import io.netty.util.internal.ConcurrentSet;

/**
 * The type gtx target holder
 *
 * @author xingfudeshi@gmail.com
 */
public enum GtxTargetHolder {
    /**
     * instance
     */
    INSTANCE;
    private Set<GtxTarget> gtxTargets;

    GtxTargetHolder() {
        this.gtxTargets = new ConcurrentSet<>();
    }

    /**
     * add
     *
     * @param gtxTarget
     * @return boolean
     * @author xingfudeshi@gmail.com
     */
    public boolean add(GtxTarget gtxTarget) {
        return this.gtxTargets.add(gtxTarget);
    }

    /**
     * contains
     *
     * @param gtxTarget
     * @return boolean
     * @author xingfudeshi@gmail.com
     */
    public boolean contains(GtxTarget gtxTarget) {
        return this.gtxTargets.contains(gtxTarget);
    }

    /**
     * remove
     *
     * @param gtxTarget
     * @return boolean
     * @author xingfudeshi@gmail.com
     */
    public boolean remove(GtxTarget gtxTarget) {
        return this.gtxTargets.remove(gtxTarget);
    }

    /**
     * find
     *
     * @param gtxTargetType
     * @param targetName
     * @return io.seata.spring.schema.GtxTarget
     * @author xingfudeshi@gmail.com
     */
    public GtxTarget find(GtxTargetType gtxTargetType, String targetName) {
        return this.gtxTargets
            .stream()
            .filter(gtxTarget -> gtxTarget.getGtxTargetType().equals(gtxTargetType) && gtxTarget.getTargetName().equals(targetName))
            .findAny().orElse(null);
    }

    /**
     * try find
     *
     * @param clasz
     * @param method
     * @return io.seata.spring.schema.GtxTarget
     * @author xingfudeshi@gmail.com
     */
    public GtxTarget tryFind(Class<?> clasz, Method method) {
        GtxTarget gtxTarget = null;
        if (clasz != null) {
            gtxTarget = GtxTargetHolder.INSTANCE.find(GtxTargetType.CLASS, clasz.getName());
        }
        if (method != null && gtxTarget == null) {
            gtxTarget = GtxTargetHolder.INSTANCE.find(GtxTargetType.METHOD, method.getName());
        }
        return gtxTarget;
    }


}
