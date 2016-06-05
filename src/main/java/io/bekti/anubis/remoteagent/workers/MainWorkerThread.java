package io.bekti.anubis.remoteagent.workers;

import io.bekti.anubis.remoteagent.messages.ExecuteMessage;
import io.bekti.anubis.remoteagent.messages.ProducerMessage;
import io.bekti.anubis.remoteagent.utils.SharedConfiguration;
import io.bekti.anubis.remoteagent.ws.AgentWebSocketHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWorkerThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private static BlockingQueue<ExecuteMessage> requests = new LinkedBlockingQueue<>();
    private ExecutorService executorService;

    public MainWorkerThread() {
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            try {
                ExecuteMessage request = requests.poll(1000, TimeUnit.MILLISECONDS);

                if (request == null) continue;

                executorService.submit(() -> {
                    execute(request);

                    Session session = AgentWebSocketHandler.getCurrentSession();

                    if (session.isOpen()) {
                        sendExecutionResult(session, request);
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        if (running.get()) {
            running.set(false);

            executorService.shutdown();
        }
    }

    private void sendExecutionResult(Session session, ExecuteMessage executeMessage) {
        try {
            ProducerMessage producerMessage = new ProducerMessage();
            producerMessage.setTopic(SharedConfiguration.getString("remote.agent.responses"));
            producerMessage.setValue(executeMessage.toJson());

            session.getRemote().sendString(producerMessage.toJson());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void execute(ExecuteMessage request) {
        StringBuffer output = new StringBuffer();

        try {
            ProcessBuilder builder = new ProcessBuilder(request.getCommand());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            process.waitFor(SharedConfiguration.getLong("max.execution.time"), TimeUnit.MILLISECONDS);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

            request.setResult(output.toString());
            request.setExitValue(process.exitValue());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void enqueueExecutionRequest(ExecuteMessage request) {
        try {
            requests.put(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
