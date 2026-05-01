package chechelpo.demo.events.types;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.EntityKey;

public record DeletedEntity(EntityTypes.Types type, EntityKey<?> key) implements Event{}
