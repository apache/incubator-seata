/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.discovery.loadbalance;

import com.alibaba.fastjson.JSON;
import io.seata.common.util.NetUtil;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class ServerRegistration implements Serializable {

    private final InetSocketAddress address;

    private final int weight;

    public ServerRegistration(String host, int port, int weight) {
        this(new InetSocketAddress(host, port), weight);
    }

    public ServerRegistration(InetSocketAddress address, int weight) {
        NetUtil.validAddress(address);
        this.address = address;
        this.weight = weight;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getStringAddress() {
        return NetUtil.toStringAddress(address);
    }

    public int getWeight() {
        return weight;
    }

    //cache
    private String json;

    public String serialize() {
        if (json != null) {
            return json;
        }
        json = JSON.toJSONString(this);
        return json;
    }

    public static ServerRegistration valueOf(String json) {
        return JSON.parseObject(json, ServerRegistration.class);
    }

    @Override
    public int hashCode() {
        if (weight != 0) {
            return address.hashCode() + weight;
        }else {
            return address.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ServerRegistration)) {
            return false;
        }
        ServerRegistration registration = (ServerRegistration) obj;
        boolean sameRegistration = false;

        if (this.address != null) {
            sameRegistration = this.address.equals(registration.address);
        }
        sameRegistration = (this.weight == registration.weight) && sameRegistration;
        return sameRegistration;
    }

    @Override
    public String toString() {
        return "ServerRegistration{" +
                "address=" + address +
                ", weight=" + weight +
                '}';
    }
}
