package io.bekti.anubis.remoteagent.messages;

import io.bekti.anubis.remoteagent.types.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseMessage {

    private static final Logger log = LoggerFactory.getLogger(BaseMessage.class);

    protected Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = null;

        try {
            json = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return json;
    }

}
