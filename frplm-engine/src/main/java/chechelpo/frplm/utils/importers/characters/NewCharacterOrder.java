package chechelpo.frplm.utils.importers.characters;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.utils.importers.lorebooks.NewEntryOrder;

import java.util.List;

public record NewCharacterOrder(EntityDataPayload<CharactersRecord> info, List<NewEntryOrder> lorebookEntries) {}
