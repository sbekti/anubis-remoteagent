package io.bekti.anubis.remoteagent.worker;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class WatchDogThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(WatchDogThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private Session session;
    private long watchDogTimeout;
    private AtomicLong lastPingTimestamp = new AtomicLong();

    public WatchDogThread(Session session, long watchDogTimeout) {
        this.session = session;
        this.watchDogTimeout = watchDogTimeout;
    }

    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            try {
                Thread.sleep(watchDogTimeout);

                log.debug("WOOF {}?", session.getRemoteAddress().getHostString());

                long currentTimestamp = System.currentTimeMillis();

                if (currentTimestamp - lastPingTimestamp.get() > watchDogTimeout) {
                    log.debug("WOOF {}!", session.getRemoteAddress().getHostString());

                    try {
                        session.disconnect();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } catch (InterruptedException ignored) {

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void setLastPingTimestamp(long timestamp) {
        lastPingTimestamp.set(timestamp);
    }

    public long getLastPingTimestamp() {
        return lastPingTimestamp.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        if (running.get()) {
            running.set(false);
            Thread.currentThread().interrupt();
        }
    }

}
