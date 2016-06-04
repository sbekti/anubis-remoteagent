package io.bekti.anubis.remoteagent.messages;

import io.bekti.anubis.remoteagent.types.Event;

public class PingMessage extends BaseMessage {

    private long watchDogTimeout;

    public PingMessage() {
        this.event = Event.PING;
    }

    public long getWatchDogTimeout() {
        return watchDogTimeout;
    }

    public void setWatchDogTimeout(long watchDogTimeout) {
        this.watchDogTimeout = watchDogTimeout;
    }

}
