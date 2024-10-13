package org.apache.seata.common.store;

public enum LockMode {
    /**
     * The File store mode.
     */
    FILE("file"),
    /**
     * The Db store mode.
     */
    DB("db"),
    /**
     * The Redis store mode.
     */
    REDIS("redis"),
    /**
     * raft store
     */
    RAFT("raft");

    private String name;

    LockMode(String name) {
        this.name = name;
    }

    public static LockMode get(String name) {
        for (LockMode mode : LockMode.values()) {
            if (mode.getName().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown lock mode:" + name);
    }

    /**
     * whether contains value of store mode
     *
     * @param name the mode name
     * @return the boolean
     */
    public static boolean contains(String name) {
        try {
            return get(name) != null ? true : false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getName() {
        return name;
    }
}