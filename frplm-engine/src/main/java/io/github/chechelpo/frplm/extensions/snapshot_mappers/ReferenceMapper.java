package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.Snapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.StableReference;
import io.github.chechelpo.frplm.extensions.api.utils.FindResult;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jooq.TableRecord;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

sealed abstract class ReferenceMapper<
        R extends TableRecord<R>,
        S extends StableReference,
        E extends Snapshot<S>
        >
        permits CharacterMapper, ConnectionMapper, EntryMapper, LocationMapper, LorebookMapper, PromptMapper, RegionMapper, SectionMapper, TagMapper, WorldMapper
{
    private final Class<E> type;
    private final EntityReader<R> reader;
    private final Function<String, S> referenceParser;
    private final Function<S, EntityKey<R>> toKey;
    private final BiFunction<R, ExtensionContext, E> toSnapshot;

    protected ReferenceMapper(
            Class<E> type,
            Function<String, S> referenceParser,
            BiFunction<R, ExtensionContext, E> toSnapshot,
            Function<S, EntityKey<R>> toKey,
            EntityReader<R> reader
    ) {
        this.type = type;
        this.reader = reader;
        this.referenceParser = referenceParser;
        this.toSnapshot = toSnapshot;
        this.toKey = toKey;
    }

    final void validate(){
         reader.validateKeyStructure(toKey.apply(getExampleReference()))
                 .ifFailureThrow(msg -> new IllegalStateException(
                         """
                         Reference mapper of type %s has an invalid toKey() function:
                         %s
                         """.formatted(
                                 type,
                                 msg
                         )
                 ));
    }

    @Contract(pure=true, value = "-> new")
    abstract S getExampleReference();

    public Class<E> type() {
        return type;
    }

    public @NotNull FindResult<E, ?, ?> resolve(ExtensionContext context, String reference) {
        return getWithReference(context, referenceParser.apply(reference));
    }

    public final @NotNull FindResult<E, ?, ?> getWithReference(ExtensionContext context, S reference){
        return reader.find(toKey.apply(reference))
                .mapResult(record -> toSnapshot.apply(record, context));
    }

    protected final @NotNull @Unmodifiable List<E> getAll(ExtensionContext context){
        return reader.getAll()
                .map(record -> toSnapshot.apply(record, context));
    }
}