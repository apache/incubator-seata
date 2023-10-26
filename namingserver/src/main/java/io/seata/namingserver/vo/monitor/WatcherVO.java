package io.seata.namingserver.vo.monitor;

import java.util.List;

public class WatcherVO {
    private String vGroup;
    private List<String> watcherIp;

    public WatcherVO() {
    }

    public WatcherVO(String vGroup, List<String> watcherIp) {
        this.vGroup = vGroup;
        this.watcherIp = watcherIp;
    }

    public String getvGroup() {
        return vGroup;
    }

    public void setvGroup(String vGroup) {
        this.vGroup = vGroup;
    }

    public List<String> getWatcherIp() {
        return watcherIp;
    }

    public void setWatcherIp(List<String> watcherIp) {
        this.watcherIp = watcherIp;
    }
}
