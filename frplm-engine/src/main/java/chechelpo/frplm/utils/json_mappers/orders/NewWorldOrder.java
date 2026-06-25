package chechelpo.frplm.utils.json_mappers.orders;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;

import java.util.List;

/** They are turning the frogs gay */
public record NewWorldOrder (
    EntityDataPayload<WorldsRecord> dataPayload,
    NewLorebookOrder lorebook,
    List<NewLocationOrder> locations,
    List<NewEdgeOrder> locationEdges
    ){}
