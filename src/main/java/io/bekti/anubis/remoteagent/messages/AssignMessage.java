package io.bekti.anubis.remoteagent.messages;

import io.bekti.anubis.remoteagent.models.KafkaPartition;
import io.bekti.anubis.remoteagent.types.Event;

import java.util.List;

public class AssignMessage extends BaseMessage {

    private List<KafkaPartition> partitions;

    public AssignMessage() {
        this.event = Event.ASSIGN;
    }

    public void setPartitions(List<KafkaPartition> partitions) {
        this.partitions = partitions;
    }

    public List<KafkaPartition> getPartitions() {
        return this.partitions;
    }

}
