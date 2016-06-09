package io.bekti.anubis.remoteagent.http;

import io.bekti.anubis.remoteagent.util.ConfigUtils;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentWebSocketClient extends Thread {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketClient.class);

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean reconnect = new AtomicBoolean(true);
    private WebSocketClient client;

    public AgentWebSocketClient() {}

    @Override
    public void run() {
        log.info("Starting agent...");
        running.set(true);

        while (running.get() && reconnect.get()) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setTrustAll(ConfigUtils.getBoolean("ssl.trust.all"));

            client = new WebSocketClient(sslContextFactory);
            AgentWebSocketHandler handler = new AgentWebSocketHandler();

            try {
                client.start();

                URI serverUri = new URI(ConfigUtils.getString("server.url"));
                ClientUpgradeRequest request = new ClientUpgradeRequest();

                Future<Session> future = client.connect(handler, serverUri, request);
                log.info("Connecting to {}", serverUri);

                while (running.get() && future.get().isOpen()) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                try {
                    client.stop();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            if (reconnect.get()) {
                try {
                    long reconnectInterval = ConfigUtils.getLong("auto.reconnect.interval");
                    Thread.sleep(reconnectInterval);
                } catch (InterruptedException ignored) {

                }
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        if (running.get()) {
            running.set(false);
            reconnect.set(false);

            try {
                client.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
