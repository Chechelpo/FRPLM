package chechelpo.demo.events.types;

public sealed interface Event permits
DeletedEntity, NewEntity, UpdatedEntity
{}
