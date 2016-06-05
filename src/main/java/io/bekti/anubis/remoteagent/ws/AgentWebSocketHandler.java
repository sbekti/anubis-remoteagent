package io.bekti.anubis.remoteagent.ws;

import io.bekti.anubis.remoteagent.messages.*;
import io.bekti.anubis.remoteagent.models.KafkaPartition;
import io.bekti.anubis.remoteagent.utils.SharedConfiguration;
import io.bekti.anubis.remoteagent.workers.MainWorkerThread;
import io.bekti.anubis.remoteagent.workers.WatchDogTimer;
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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@WebSocket
public class AgentWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    private ScheduledThreadPoolExecutor watchDogTimer;
    private AtomicLong lastPingTimestamp = new AtomicLong();
    private static Session currentSession;

    public AgentWebSocketHandler() {}

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("Connected to server: {}", session.getRemoteAddress().getAddress());
        currentSession = session;

        subscribe(session);
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
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
        try {
            if (frame.getType() == Frame.Type.PING) {
                lastPingTimestamp.set(System.currentTimeMillis());

                ByteBuffer payload = frame.getPayload();
                String stringPayload = BufferUtil.toString(payload);
                log.debug("Got PING: {}", stringPayload);

                PingMessage pingMessage = new ObjectMapper().readValue(stringPayload, PingMessage.class);

                if (watchDogTimer == null) {
                    long watchDogTimeout = pingMessage.getWatchDogTimeout();
                    createWatchDogTimer(session, watchDogTimeout);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static Session getCurrentSession() {
        return currentSession;
    }

    private void subscribe(Session session) {
        String topic = SharedConfiguration.getString("remote.agent.requests");
        String groupId = SharedConfiguration.getString("group.id");

        SubscribeMessage subscribeMessage = new SubscribeMessage();
        subscribeMessage.setTopics(Collections.singletonList(topic));
        subscribeMessage.setGroupId(groupId);

        try {
            session.getRemote().sendString(subscribeMessage.toJson());
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
