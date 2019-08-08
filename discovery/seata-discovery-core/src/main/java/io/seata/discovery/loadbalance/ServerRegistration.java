package io.seata.discovery.loadbalance;

import java.net.InetSocketAddress;

public class ServerRegistration {

    private InetSocketAddress address;

    private int weight;

    public ServerRegistration(InetSocketAddress address, int weight) {
        this.address = address;
        this.weight = weight;
    }

    public ServerRegistration(String serialfrom) {

    }

    public String serialString() {
        return address + "-" + weight;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
