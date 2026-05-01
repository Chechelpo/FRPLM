package chechelpo.demo.events.types;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.EntityDataPayload;

public record UpdatedEntity(EntityTypes.Types type, EntityDataPayload<?> newData) implements Event{}
