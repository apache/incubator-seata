package io.seata.core.model;

/**
 * The enum of two phase rollback type.
 */
public enum RollbackType {

    /**
     * The sync rollback.
     */
    SyncRollback(0),

    /**
     * The async rollback.
     */
    AsyncRollback(1),

    /**
     * The no rollback.
     */
    NoRollback(2),
    ;

    private int value;

    RollbackType(int value) {
        this.value = value;
    }

    /**
     * Get value.
     *
     * @return the value
     */
    public int value(){
        return value;
    }

    /**
     * Get rollback type.
     *
     * @param value the value
     * @return the rollback type
     */
    public static RollbackType get(byte value){
        return get((int) value);
    }

    /**
     * Get rollback type.
     *
     * @param value the value
     * @return the rollback type
     */
    public static RollbackType get(int value) {
        for (RollbackType t : RollbackType.values()) {
            if (t.value() == value) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown RollbackType[" + value + "]");
    }

    /**
     * Get the default by branch type.
     *
     * @param branchType the branch type
     * @return the default rollback type
     */
    public static RollbackType getDefault(BranchType branchType) {
        if (branchType == BranchType.SAGA) {
            return NoRollback;
        } else {
            return SyncRollback;
        }
    }
}
