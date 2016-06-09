package io.bekti.anubis.remoteagent.worker;

import io.bekti.anubis.remoteagent.model.message.ExecuteMessage;
import io.bekti.anubis.remoteagent.model.message.ProducerMessage;
import io.bekti.anubis.remoteagent.util.ConfigUtils;
import io.bekti.anubis.remoteagent.http.AgentWebSocketHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWorkerThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(MainWorkerThread.class);
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
            producerMessage.setTopic(ConfigUtils.getString("remote.agent.responses"));
            producerMessage.setValue(executeMessage.toJson());

            session.getRemote().sendString(producerMessage.toJson());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void execute(ExecuteMessage executeMessage) {
        StringBuffer output = new StringBuffer();

        try {
            ProcessBuilder builder = new ProcessBuilder(executeMessage.getCommand());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            long now = System.currentTimeMillis();
            long timeout = executeMessage.getTimeout();
            long finish = now + timeout;
            boolean running = true;

            while (running && process.isAlive()) {
                String line;

                while (running && (line = reader.readLine()) != null) {
                    output.append(line + "\n");

                    Thread.sleep(10);

                    if (timeout <= 0) continue;

                    if (System.currentTimeMillis() > finish) {
                        process.destroy();
                        running = false;
                    }
                }
            }

            process.waitFor();

            executeMessage.setResult(output.toString());
            executeMessage.setExitValue(process.exitValue());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void enqueueExecutionRequest(ExecuteMessage request) {
        requests.add(request);
    }

}
