package io.bekti.anubis.remoteagent.http;

import io.bekti.anubis.remoteagent.model.message.*;
import io.bekti.anubis.remoteagent.model.kafka.KafkaPartition;
import io.bekti.anubis.remoteagent.util.SharedConfiguration;
import io.bekti.anubis.remoteagent.worker.MainWorkerThread;
import io.bekti.anubis.remoteagent.worker.WatchDogThread;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebSocket
public class AgentWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    private WatchDogThread watchDogThread;
    private static Session currentSession;

    public AgentWebSocketHandler() {}

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("Connected to server: {}", session.getRemoteAddress().getAddress());
        currentSession = session;

        authenticate();
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        processMessage(message);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log.info("Connection closed: {} - {}", statusCode, reason);

        currentSession = null;
        destroyWatchDogThread();
    }

    @OnWebSocketFrame
    public void onFrame(Session session, Frame frame) {
        try {
            if (frame.getType() == Frame.Type.PING) {
                ByteBuffer payload = frame.getPayload();
                String stringPayload = BufferUtil.toString(payload);
                log.debug("Got PING: {}", stringPayload);

                PingMessage pingMessage = new ObjectMapper().readValue(stringPayload, PingMessage.class);

                if (watchDogThread == null) {
                    createWatchDogThread(pingMessage.getWatchDogTimeout());
                }

                watchDogThread.setLastPingTimestamp(System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void processMessage(String message) {
        try {
            JsonNode payload = new ObjectMapper().readValue(message, JsonNode.class);
            String event = payload.get("event").asText();

            switch (event) {
                case "assign":
                    AssignMessage assignMessage = new ObjectMapper().readValue(message, AssignMessage.class);
                    processAssignMessage(assignMessage);
                    break;
                case "auth":
                    AuthMessage authMessage = new ObjectMapper().readValue(message, AuthMessage.class);
                    processAuthMessage(authMessage);
                    break;
                case "revoke":
                    RevokeMessage revokeMessage = new ObjectMapper().readValue(message, RevokeMessage.class);
                    processRevokeMessage(revokeMessage);
                    break;
                case "message":
                    String value = payload.get("value").asText();
                    ExecuteMessage executeMessage = new ObjectMapper().readValue(value, ExecuteMessage.class);
                    processExecuteMessage(executeMessage);
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void processAssignMessage(AssignMessage assignMessage) {
        List<String> assignments = new ArrayList<>();

        for (KafkaPartition partition : assignMessage.getPartitions()) {
            assignments.add(partition.getTopic() + "-" + partition.getId());
        }

        log.info("Got partition assignment: {}", assignments);
    }

    private void processRevokeMessage(RevokeMessage revokeMessage) {
        List<String> revocations = new ArrayList<>();

        for (KafkaPartition partition : revokeMessage.getPartitions()) {
            revocations.add(partition.getTopic() + "-" + partition.getId());
        }

        log.info("Got partition revocation: {}", revocations);
    }

    private void processExecuteMessage(ExecuteMessage executeMessage) {
        String ownNodeId = SharedConfiguration.getString("node.id");
        if (!executeMessage.getNodeId().equals(ownNodeId)) return;

        log.info("Got execution request: {}", executeMessage.toJson());
        MainWorkerThread.enqueueExecutionRequest(executeMessage);
    }

    private void processAuthMessage(AuthMessage authMessage) {
        if (authMessage.isSuccess()) {
            log.info("Successfully authenticated to server");
            subscribe();
        } else {
            log.error("Auth error: {}", authMessage.getMessage());
        }
    }

    private void createWatchDogThread(long watchDogTimeout) {
        destroyWatchDogThread();

        watchDogThread = new WatchDogThread(currentSession, watchDogTimeout);
        watchDogThread.start();
    }

    private void destroyWatchDogThread() {
        if (watchDogThread == null) return;
        if (!watchDogThread.isRunning()) return;

        watchDogThread.shutdown();

        try {
            watchDogThread.join();
            watchDogThread = null;
        } catch (InterruptedException ignored) {

        }
    }

    private void subscribe() {
        String topic = SharedConfiguration.getString("remote.agent.requests");
        String groupId = SharedConfiguration.getString("group.id");

        SubscribeMessage subscribeMessage = new SubscribeMessage();
        subscribeMessage.setTopics(Collections.singletonList(topic));
        subscribeMessage.setGroupId(groupId);

        try {
            currentSession.getRemote().sendString(subscribeMessage.toJson());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void authenticate() {
        String token = SharedConfiguration.getString("access.token");

        AuthMessage authMessage = new AuthMessage();
        authMessage.setToken(token);

        try {
            currentSession.getRemote().sendString(authMessage.toJson());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static Session getCurrentSession() {
        return currentSession;
    }

}
