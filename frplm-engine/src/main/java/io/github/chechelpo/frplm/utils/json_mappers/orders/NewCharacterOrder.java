package io.github.chechelpo.frplm.utils.json_mappers.orders;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;

public record NewCharacterOrder(
        EntityDataPayload<CharactersRecord> info,
        NewLorebookOrder lorebook
) {}
