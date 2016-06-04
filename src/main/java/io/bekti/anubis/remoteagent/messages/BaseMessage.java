package io.bekti.anubis.remoteagent.messages;

import com.google.gson.Gson;
import io.bekti.anubis.remoteagent.types.Event;

public class BaseMessage {

    protected Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

}
