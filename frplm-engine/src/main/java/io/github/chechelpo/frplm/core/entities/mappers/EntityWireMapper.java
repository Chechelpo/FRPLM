package io.github.chechelpo.frplm.core.entities.mappers;

import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.IO.ZipReader;
import io.github.chechelpo.frplm.utils.orders.CreationOrder;
import org.jooq.TableRecord;
import tools.jackson.databind.JsonNode;

import java.util.List;

public sealed interface EntityWireMapper<R extends TableRecord<R>, J, O extends CreationOrder<R>> permits ABSWireMapper {
    default List<J> jsonRecordsFrom(List<R> records, ZipBuilder builder){
        return records.stream()
                .map(record -> jsonRecordFrom(record, builder))
                .toList();
    }
    J jsonRecordFrom(R record, ZipBuilder builder);
    JsonNode jsonFrom(R record, ZipBuilder builder);

    default List<O> ordersFrom(List<J> jsons){
        return jsons.stream()
                .map(this::orderFrom)
                .toList();
    }

    O orderFrom(J json);
}
