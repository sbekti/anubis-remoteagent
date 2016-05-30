package io.bekti.anubis.remoteagent.ws;

import io.bekti.anubis.remoteagent.utils.SharedConfiguration;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentWebSocketServer extends Thread {

    private static Logger log = LoggerFactory.getLogger(AgentWebSocketServer.class);
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean autoReconnect = new AtomicBoolean(true);

    private WebSocketClient client;

    public AgentWebSocketServer() {}

    @Override
    public void run() {
        log.info("Starting agent...");
        running.set(true);

        while (running.get() && autoReconnect.get()) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setTrustAll(true);

            client = new WebSocketClient(sslContextFactory);
            AgentWebSocketHandler handler = new AgentWebSocketHandler();

            try {
                client.start();

                URI serverUri = new URI(SharedConfiguration.getString("server.url"));
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

            if (autoReconnect.get()) {
                try {
                    long reconnectInterval = SharedConfiguration.getLong("auto.reconnect.interval");
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
            autoReconnect.set(false);

            try {
                client.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
