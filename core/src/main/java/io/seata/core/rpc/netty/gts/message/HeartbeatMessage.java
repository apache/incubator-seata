package io.seata.core.rpc.netty.gts.message;


import java.io.Serializable;

public class HeartbeatMessage implements Serializable {
    private static final long serialVersionUID = -985316399527884899L;
    private boolean pingOrPong = true;
    public static HeartbeatMessage PING = new HeartbeatMessage(true);
    public static HeartbeatMessage PONG = new HeartbeatMessage(false);

    private HeartbeatMessage(boolean pingOrPong) {
        this.pingOrPong = pingOrPong;
    }

    @Override
    public String toString() {
        return this.pingOrPong ? "TXC ping" : "TXC pong";
    }
}