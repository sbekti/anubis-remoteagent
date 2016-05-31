package io.bekti.anubis.remoteagent.ws;

import io.bekti.anubis.remoteagent.types.ExecutionRequest;
import io.bekti.anubis.remoteagent.utils.SharedConfiguration;
import io.bekti.anubis.remoteagent.workers.MainWorkerThread;
import io.bekti.anubis.remoteagent.workers.WatchDogTimer;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@WebSocket
public class AgentWebSocketHandler {

    private static Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    private ScheduledThreadPoolExecutor watchDogTimer;
    private AtomicLong lastPingTimestamp = new AtomicLong();
    private static Session currentSession;

    public AgentWebSocketHandler() {}

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("Connected to server: {}", session);
        currentSession = session;

        subscribe(session);
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        log.info("Received message: {}", message);

        processMessage(message);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log.info("Connection closed: {} - {}", statusCode, reason);
        currentSession = null;
        destroyWatchDogTimer();
    }

    @OnWebSocketFrame
    public void onFrame(Session session, Frame frame) {
        if (frame.getType() == Frame.Type.PING) {
            lastPingTimestamp.set(System.currentTimeMillis());

            ByteBuffer payload = frame.getPayload();
            String stringPayload = BufferUtil.toString(payload);
            log.info("Got PING: {}", stringPayload);

            JSONObject pingPayload = new JSONObject(stringPayload);

            if (watchDogTimer == null) {
                long watchDogTimeout = pingPayload.getLong("watchDogTimeout");
                createWatchDogTimer(session, watchDogTimeout);
            }
        }
    }

    public static Session getCurrentSession() {
        return currentSession;
    }

    private void subscribe(Session session) {
        String topic = SharedConfiguration.getString("remote.agent.requests");
        String groupId = SharedConfiguration.getString("group.id");

        JSONObject payload = new JSONObject();
        payload.put("event", "subscribe");
        payload.put("topics", new JSONArray().put(topic));
        payload.put("groupId", groupId);

        try {
            session.getRemote().sendString(payload.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void processMessage(String message) {
        try {
            JSONObject body = new JSONObject(message);

            if (body.getString("event").equals("ping")) return;

            JSONObject payload = new JSONObject(body.getString("value"));
            String nodeId = payload.getString("nodeId");
            String requestId = payload.getString("requestId");
            JSONArray command = payload.getJSONArray("command");

            String ownNodeId = SharedConfiguration.getString("node.id");

            if (!nodeId.equals(ownNodeId)) return;

            ArrayList<String> commandArgs = new ArrayList<>();

            for (int i = 0; i < command.length(); ++i) {
                commandArgs.add(command.getString(i));
            }

            ExecutionRequest request = new ExecutionRequest(nodeId, requestId, commandArgs);
            MainWorkerThread.enqueueExecutionRequest(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void createWatchDogTimer(Session session, long watchDogTimeout) {
        lastPingTimestamp.set(System.currentTimeMillis());

        watchDogTimer = new ScheduledThreadPoolExecutor(1);

        watchDogTimer.scheduleAtFixedRate(
                new WatchDogTimer(session, lastPingTimestamp, watchDogTimeout),
                watchDogTimeout,
                watchDogTimeout,
                TimeUnit.MILLISECONDS
        );
    }

    private void destroyWatchDogTimer() {
        watchDogTimer.shutdown();
    }

}
