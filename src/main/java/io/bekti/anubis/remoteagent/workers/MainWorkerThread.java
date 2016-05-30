package io.bekti.anubis.remoteagent.workers;

import io.bekti.anubis.remoteagent.types.ExecutionRequest;
import io.bekti.anubis.remoteagent.utils.SharedConfiguration;
import io.bekti.anubis.remoteagent.ws.AgentWebSocketHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWorkerThread extends Thread {

    private static Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private static BlockingQueue<ExecutionRequest> requests = new LinkedBlockingQueue<>();
    private ExecutorService executorService;

    public MainWorkerThread() {
        executorService = Executors.newFixedThreadPool(SharedConfiguration.getInteger("max.worker.threads"));
    }

    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            try {
                ExecutionRequest request = requests.poll(1000, TimeUnit.MILLISECONDS);

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
        }
    }

    private void sendExecutionResult(Session session, ExecutionRequest request) {
        try {
            JSONObject response = new JSONObject();
            response.put("nodeId", request.getNodeId());
            response.put("requestId", request.getRequestId());
            response.put("command", new JSONArray(request.getCommand()));
            response.put("result", request.getResult());
            response.put("exitValue", request.getExitValue());

            JSONObject payload = new JSONObject();
            payload.put("event", "publish");
            payload.put("topic", SharedConfiguration.getString("remote.agent.responses"));
            payload.put("value", response.toString());

            session.getRemote().sendString(payload.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void execute(ExecutionRequest request) {
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

    public static void enqueueExecutionRequest(ExecutionRequest request) {
        try {
            requests.put(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
