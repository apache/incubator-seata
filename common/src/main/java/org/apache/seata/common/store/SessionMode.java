package org.apache.seata.common.store;

public enum SessionMode {
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

    SessionMode(String name) {
        this.name = name;
    }

    public static SessionMode get(String name) {
        for (SessionMode mode : SessionMode.values()) {
            if (mode.getName().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown session mode:" + name);
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