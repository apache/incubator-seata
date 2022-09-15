package io.seata.server.console.vo;
/**
 * GlobalLockVO
 * @author Yuzhiqiang
 */
public class GlobalConfigVO {
    private String id;
    private String name;
    private String value;
    private String descr;
    public GlobalConfigVO(){}
    public GlobalConfigVO(String id, String name, String value, String descr) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.descr = descr;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }
}
