package io.bekti.anubis.remoteagent.model.message;

import io.bekti.anubis.remoteagent.model.kafka.KafkaPartition;

import java.util.List;

public class RevokeMessage extends BaseMessage {

    private List<KafkaPartition> partitions;

    public RevokeMessage() {
        this.event = MessageEvent.REVOKE;
    }

    public void setPartitions(List<KafkaPartition> partitions) {
        this.partitions = partitions;
    }

    public List<KafkaPartition> getPartitions() {
        return this.partitions;
    }

}
