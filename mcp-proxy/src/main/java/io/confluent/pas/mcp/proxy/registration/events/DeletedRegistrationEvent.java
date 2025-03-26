package io.confluent.pas.mcp.proxy.registration.events;

import io.confluent.pas.mcp.common.services.Schemas;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event for deleted registration.
 */
@Getter
public class DeletedRegistrationEvent extends ApplicationEvent {

    private final Schemas.Registration registration;

    public DeletedRegistrationEvent(Object source, Schemas.Registration registration) {
        super(source);
        this.registration = registration;
    }

}
