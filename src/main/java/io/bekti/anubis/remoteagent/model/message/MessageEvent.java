package io.bekti.anubis.remoteagent.model.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MessageEvent {

    @JsonProperty("assign")
    ASSIGN,

    @JsonProperty("auth")
    AUTH,

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
