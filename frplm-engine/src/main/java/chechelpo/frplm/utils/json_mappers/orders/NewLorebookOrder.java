package chechelpo.frplm.utils.json_mappers.orders;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;

import java.util.List;

public record NewLorebookOrder (
    EntityDataPayload<LorebooksRecord> entityPayload,
    List<NewEntryOrder> entries
){}
