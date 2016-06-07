package io.bekti.anubis.remoteagent;

import io.bekti.anubis.remoteagent.worker.MainWorkerThread;
import io.bekti.anubis.remoteagent.http.AgentWebSocketClient;
import io.bekti.anubis.remoteagent.util.SharedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAgentMain {

    private static final Logger log = LoggerFactory.getLogger(RemoteAgentMain.class);

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        SharedConfiguration.loadFromClassPath();

        AgentWebSocketClient client = new AgentWebSocketClient();
        client.start();

        MainWorkerThread worker = new MainWorkerThread();
        worker.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Shutting down...");

            try {
                client.shutdown();
                worker.shutdown();

                client.join();
                worker.join();
                mainThread.join();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }));
    }

}
