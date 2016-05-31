package io.bekti.anubis.remoteagent;

import io.bekti.anubis.remoteagent.workers.MainWorkerThread;
import io.bekti.anubis.remoteagent.ws.AgentWebSocketClient;
import io.bekti.anubis.remoteagent.utils.SharedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAgentMain {

    private static Logger log = LoggerFactory.getLogger(RemoteAgentMain.class);

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        SharedConfiguration.loadFromFile(System.getProperty("config"));

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
