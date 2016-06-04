package io.bekti.anubis.remoteagent.messages;

import io.bekti.anubis.remoteagent.models.KafkaPartition;
import io.bekti.anubis.remoteagent.types.Event;

import java.util.List;

public class RevokeMessage extends BaseMessage {

    private List<KafkaPartition> partitions;

    public RevokeMessage() {
        this.event = Event.REVOKE;
    }

    public void setPartitions(List<KafkaPartition> partitions) {
        this.partitions = partitions;
    }

    public List<KafkaPartition> getPartitions() {
        return this.partitions;
    }

}
