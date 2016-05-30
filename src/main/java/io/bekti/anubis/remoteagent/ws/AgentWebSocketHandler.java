package io.bekti.anubis.remoteagent.ws;

import java.util.ArrayList;

import io.bekti.anubis.remoteagent.types.ExecutionRequest;
import io.bekti.anubis.remoteagent.utils.SharedConfiguration;
import io.bekti.anubis.remoteagent.workers.MainWorkerThread;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class AgentWebSocketHandler {

    private static Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);
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

    public static Session getCurrentSession() {
        return currentSession;
    }

}
