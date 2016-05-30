package io.bekti.anubis.remoteagent;

import io.bekti.anubis.remoteagent.workers.MainWorkerThread;
import io.bekti.anubis.remoteagent.ws.AgentWebSocketServer;
import io.bekti.anubis.remoteagent.utils.SharedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAgentMain {

    private static Logger log = LoggerFactory.getLogger(RemoteAgentMain.class);

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        SharedConfiguration.loadFromFile(System.getProperty("config"));

        AgentWebSocketServer server = new AgentWebSocketServer();
        server.start();

        MainWorkerThread worker = new MainWorkerThread();
        worker.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Shutting down...");

            try {
                server.shutdown();
                worker.shutdown();

                server.join();
                worker.join();
                mainThread.join();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }));
    }

}
