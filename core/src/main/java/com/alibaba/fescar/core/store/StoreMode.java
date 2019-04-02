package com.alibaba.fescar.core.store;

/**
 * transaction log store mode
 *
 * @author zhangsen
 * @data 2019 /4/2
 */
public enum StoreMode {

    /**
     * file store
     */
    FILE,

    /**
     * database store
     */
    DB;

    /**
     * Valueof store mode.
     *
     * @param mode the mode
     * @return the store mode
     */
    public StoreMode valueof(String mode){
        for(StoreMode sm : values()){
            if(sm.name().equalsIgnoreCase(mode)){
                return sm;
            }
        }
        return null;
    }

}
