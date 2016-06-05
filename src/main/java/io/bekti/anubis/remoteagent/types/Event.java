package io.bekti.anubis.remoteagent.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Event {

    @JsonProperty("assign")
    ASSIGN,

    @JsonProperty("execute")
    EXECUTE,

    @JsonProperty("ping")
    PING,

    @JsonProperty("publish")
    PUBLISH,

    @JsonProperty("revoke")
    REVOKE,

    @JsonProperty("subscribe")
    SUBSCRIBE

}
