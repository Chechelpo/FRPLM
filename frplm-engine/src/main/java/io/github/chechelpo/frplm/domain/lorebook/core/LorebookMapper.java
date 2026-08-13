package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.entry.EntryMapper;
import io.github.chechelpo.frplm.domain.lorebook.outlet.OutletService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.IO.ZipReader;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public final class LorebookMapper extends ABSWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> {
    private final EntryMapper entryMapper;
    private final EntityReaders entityReaders;
    private final OutletService outletService;

    LorebookMapper(ObjectMapper mapper, EntryMapper entryMapper, EntityReaders entityReaders, OutletService outletService){
        super(mapper, LorebookJSON.class, null);
        this.entryMapper = entryMapper;
        this.entityReaders = entityReaders;
        this.outletService = outletService;
    }

    @Override
    protected String getZipPath(LorebookJSON json) {
        throw new UnsupportedOperationException();
    }


    @Override
    @Contract("_,_ -> new")
    protected @NonNull LorebookJSON internalRecordFrom(@NonNull LorebooksRecord record, @NonNull ZipBuilder zipBuilder) {
        return new LorebookJSON(
                record.getName(),
                fetchDefaultOutlet(record),
                entityReaders.entries().getMatching(EntityDataPayload.of(ENTRY.LOREBOOK_ID, record.getId()))
                        .stream()
                        .map(entryRecord -> entryMapper.jsonRecordFrom(entryRecord, zipBuilder))
                        .toList()
        );
    }

    @NonNull String fetchDefaultOutlet(@NonNull LorebooksRecord record){
        return outletService.getOutletName(record.getDefaultOutletId())
                .orElseThrow(() -> new EntityNotFound("No default outlet name with id: " + record.getDefaultOutletId(), Severity.SYSTEM));
    }

    @Override
    @NonNull
    protected NewLorebookOrder internalOrderFrom(@NonNull LorebookJSON json) {
        int outletId = outletService.getOrCreateOutlet(json.default_outlet_id());
        return new NewLorebookOrder(
                EntityDataPayload.<LorebooksRecord>builder()
                        .set(LOREBOOKS.NAME, json.name())
                        .set(LOREBOOKS.DEFAULT_OUTLET_ID, outletId)
                        .build(),
                json.entries().stream()
                        .map(entryMapper::orderFrom)
                        .toList()
        );
    }
}
