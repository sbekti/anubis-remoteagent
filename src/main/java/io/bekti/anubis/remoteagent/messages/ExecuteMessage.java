package io.bekti.anubis.remoteagent.messages;

import io.bekti.anubis.remoteagent.types.Event;
import java.util.List;

public class ExecuteMessage extends BaseMessage {

    private String nodeId;
    private String requestId;
    private List<String> command;
    private String result;
    private int exitValue;

    public ExecuteMessage() {
        this.event = Event.EXECUTE;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<String> getCommand() {
        return command;
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getExitValue() {
        return exitValue;
    }

    public void setExitValue(int exitValue) {
        this.exitValue = exitValue;
    }

}
