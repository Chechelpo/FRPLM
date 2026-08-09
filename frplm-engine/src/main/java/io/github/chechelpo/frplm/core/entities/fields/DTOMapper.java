package io.github.chechelpo.frplm.core.entities.fields;

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


    enum DATA_CONSTRUCTION_MODE {
        QUERY,
        CREATE,
        UPDATE
    }
    EntityDataPayload<R> getDataFrom(Map<String, Object> params, DATA_CONSTRUCTION_MODE mode);

    enum KEY_CONSTRUCTION_MODE{
        FULL_KEY,
        PARTIAL_KEY
    }
    EntityKey<R> getKeyFromDTO(Map<String, Object> params, KEY_CONSTRUCTION_MODE mode);
}
