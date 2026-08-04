package io.github.chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.TableRecord;

import java.util.List;
import java.util.Map;

public interface DTOMapper<R extends TableRecord<R>> extends FieldValidator<R> {
    default EntityDTO[] wrapRecords(List<R> records) {
        return records.stream()
                .map(this::wrapRecord)
                .toArray(EntityDTO[]::new);
    }
    EntityDTO wrapRecord(R record);
    EntityDataPayload<R> getDataFrom(Map<String, Object> params, boolean expectKeys);
    EntityKey<R> getKeyFromDTO(Map<String, Object> params, boolean expectFullKey);
}
