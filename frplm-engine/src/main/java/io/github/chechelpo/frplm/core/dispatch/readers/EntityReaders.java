package io.github.chechelpo.frplm.core.dispatch.readers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import org.jooq.Table;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public final class EntityReaders {
    private record ReaderPair<R extends TableRecord<R>>(Table<R> table, EntityReader<R> reader){
        static <R extends TableRecord<R>> ReaderPair<R> pairOf(EntityReader<R> reader){
            Objects.requireNonNull(reader);
            return new ReaderPair<>(reader.getTable(), reader);
        }
    }

    private final List<ReaderPair<?>> readers;
    private final Readers.Record record;

    EntityReaders(Readers.Record record){
        this.readers = record.asList().stream()
                .<ReaderPair<?>>map(ReaderPair::pairOf)
                .toList();
        this.record = record;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Runtime dispatch
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public <R extends TableRecord<R>> @NonNull EntityReader<R> readerFor(@NonNull R record){
        return readerFor(record.getTable());
    }

    @SuppressWarnings("unchecked")
    public <R extends TableRecord<R>> @NonNull EntityReader<R> readerFor(Table<R> table){
        return readers.stream()
                .filter(reader -> reader.table().equals(table))
                .findFirst()
                .map(pair -> (EntityReader<R>) pair.reader)
                .orElseThrow(() -> new UnexpectedException("Couldn't find reader for " + table, Severity.SYSTEM));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Compile dispatch
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public @NonNull EntityReader<LlmConnectionRecord> connections() {
        return record.connections();
    }

    public @NonNull EntityReader<ApiHostsRecord> hosts() {
        return record.hosts();
    }

    public @NonNull EntityReader<ApiKeysRecord> secrets() {
        return record.secrets();
    }
    

    public @NonNull EntityReader<CharactersRecord> characters() {
        return record.characters();
    }


    public @NonNull EntityReader<StartingLocationsRecord> startingLocations() {
        return record.startingLocations();
    }

    public @NonNull EntityReader<WorldsRecord> worlds() {
        return record.worlds();
    }

    public @NonNull EntityReader<RegionRecord> regions() {
        return record.regions();
    }

    public @NonNull EntityReader<LocationsRecord> locations() {
        return record.locations();
    }

    public @NonNull EntityReader<LocationEdgesRecord> edges() {
        return record.edges();
    }

    public @NonNull EntityReader<LorebooksRecord> lorebooks() {
        return record.lorebooks();
    }

    public @NonNull EntityReader<EntryRecord> entries() {
        return record.entries();
    }

    public @NonNull EntityReader<KeywordRecord> keywords() {
        return record.keywords();
    }

    public @NonNull EntityReader<EntryKeywordsRecord> entryKeywords() {
        return record.entryKeywords();
    }

    public @NonNull EntityReader<OutletRecord> outlets() {
        return record.outlets();
    }

    public @NonNull EntityReader<PromptTemplateRecord> templates() {
        return record.templates();
    }

    public @NonNull EntityReader<PromptSectionRecord> sections() {
        return record.sections();
    }

    public @NonNull EntityReader<SessionsRecord> sessions(){
        return record.sessions();
    }

    public @NonNull EntityReader<MessagesRecord> messages(){
        return record.messages();
    }
}
