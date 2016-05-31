package io.bekti.anubis.remoteagent.workers;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class WatchDogTimer implements Runnable {

    private Logger log = LoggerFactory.getLogger(WatchDogTimer.class);
    private Session session;
    private AtomicLong lastPingTimestamp;
    private long pingTimeout;

    public WatchDogTimer(Session session, AtomicLong lastPingTimestamp, long pingTimeout) {
        this.session = session;
        this.lastPingTimestamp = lastPingTimestamp;
        this.pingTimeout = pingTimeout;
    }

    @Override
    public void run() {
        log.info("WOOF?");

        long currentTimestamp = System.currentTimeMillis();

        if (currentTimestamp - lastPingTimestamp.get() > pingTimeout) {
            log.info("WOOF!");

            try {
                session.disconnect();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
