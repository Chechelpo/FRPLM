package io.github.chechelpo.frplm.extensions;

import io.github.chechelpo.frplm.extensions.implementations.standalone.*;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.extensions.api.EngineRepository;
import io.github.chechelpo.frplm.extensions.api.standalone.*;
import io.github.chechelpo.frplm.extensions.implementations.standalone.*;
import io.github.chechelpo.frplm.extensions.implementations.standalone.*;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class ExtensionRepository implements EngineRepository {
    private final ExtensionContext context;
    private final Map<Class<?>, Function<String, Optional<?>>> resolvers;

    public ExtensionRepository(ExtensionContext context) {
        this.context = context;
        this.resolvers = Map.ofEntries(
                entry(ConnectionSnapshot.class,         ref -> this.getConnection(ConnectionSnapshot.Reference.fromString(ref))),
                entry(CharacterSnapshot.class,          ref -> this.getCharacter(CharacterSnapshot.Reference.fromString(ref))),
                entry(PromptSnapshot.class,             ref -> this.getPrompt(PromptSnapshot.Reference.fromString(ref))),
                entry(WorldSnapshot.class,              ref -> this.getWorld(WorldSnapshot.Reference.fromString(ref))),
                entry(LorebookSnapshot.class,           ref -> this.getLorebook(LorebookSnapshot.Reference.fromString(ref))),
                entry(RegionSnapshot.class,             ref -> this.getRegion(RegionSnapshot.Reference.fromString(ref))),
                entry(LocationSnapshot.class,           ref -> this.getLocation(LocationSnapshot.Reference.fromString(ref))),
                entry(EntrySnapshot.class,              ref -> this.getEntry(EntrySnapshot.Reference.fromString(ref))),
                entry(PromptSectionEntitySnapshot.class, ref -> this.getPromptSection(PromptSectionEntitySnapshot.Reference.fromString(ref)))
        );
    }

    ExtensionContext getContext() {
        return context;
    }

    /**
     * Registers a resolver so that the return type is statically tied to the class.
     * The cast widens to {@code Optional<?>} for storage, then narrows back to
     * {@code Optional<T>} in {@link #get} — safe because this method guarantees
     * the class/resolver pairing at registration time.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S extends Snapshot<?>> Map.Entry<Class<?>, Function<String, Optional<?>>> entry(
            Class<S> type, Function<String, Optional<? extends S>> resolver) {
        Function<String, Optional<?>> widened = (Function<String, Optional<?>>) (Function) resolver;
        return Map.entry(type, widened);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NotNull Class<T> type, @NotNull String reference) {
        var resolver = resolvers.get(type);
        return resolver != null ? (Optional<T>) resolver.apply(reference) : Optional.empty();
    }

    @Override
    public @Unmodifiable @NotNull List<CharacterSnapshot> getCharacters() {
        return context.characters().getAll().stream()
                .map(record -> new CharacterImpl(record, context))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<CharacterSnapshot> getCharacterWithName(String name) {
        return context.characters().getOneMatching(CHARACTERS.NAME, name)
                .ifMoreThanOneThrow()
                .asOptional()
                .map(record -> new CharacterImpl(record, context));
    }

    @Override
    public Optional<CharacterSnapshot> getCharacter(CharacterSnapshot.Reference ref) {
        return context.characters()
                .find(EntityKey.of(CHARACTERS.ID, ref.id()))
                .map(record -> new CharacterImpl(record, context));
    }

    @Override
    public @NotNull List<ConnectionSnapshot> getConnections() {
        return context.connections().getAll().stream()
                .map(record -> (ConnectionSnapshot) new ConnectionImpl(record, context))
                .toList();
    }

    @Override
    public Optional<ConnectionSnapshot> getConnectionWithName(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<ConnectionSnapshot> getConnection(ConnectionSnapshot.Reference reference) {
        return context.connections().find(EntityKey.of(LLM_CONNECTION.ID, reference.id()))
                .map(record -> (ConnectionSnapshot) new ConnectionImpl(record, context));
    }

    @Override
    public @Unmodifiable @NotNull List<WorldSnapshot> getWorlds() {
        return context.worlds().getAll().stream()
                .map(record -> new WorldImpl(record, context))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NotNull Optional<WorldSnapshot> getWorldWithName(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<WorldSnapshot> getWorld(WorldSnapshot.@NotNull Reference reference) {
        return context.worlds().find(EntityKey.of(WORLDS.ID, reference.id())).map(record -> new WorldImpl(record, context));
    }

    @Override
    public List<LorebookSnapshot> getLorebooks() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Optional<LorebookSnapshot> getLorebookWithName(String name) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public @Unmodifiable @NotNull List<PromptSnapshot> getPrompts() {
        return context.templates().getAll().stream()
                .map(record -> (PromptSnapshot) new PromptImpl(record, context))
                .toList();
    }

    @Contract(pure = true)
    @Override
    public @NotNull Optional<PromptSnapshot> getPromptWithName(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<PromptSnapshot> getPrompt(PromptSnapshot.@NotNull Reference reference) {
        return context.templates().find(
                EntityKey.of(PROMPT_TEMPLATE.ID, Integer.valueOf(reference.id()).shortValue())
        ).map(record -> (PromptSnapshot) new PromptImpl(record, context));
    }

    @Override
    public Optional<LorebookSnapshot> getLorebook(LorebookSnapshot.Reference reference) {
        return context.lorebooks()
                .find(EntityKey.of(LOREBOOKS.ID, reference.id()))
                .map(record -> new LorebookImpl(record, context));
    }

    @Override
    public List<RegionSnapshot> getRegions() {
        return context.regions().getAll().stream()
                .map(record -> new RegionImpl(record, context))
                .map(record -> (RegionSnapshot) record)
                .toList();
    }

    @Override
    public Optional<RegionSnapshot> getRegion(RegionSnapshot.Reference reference) {
        return context.regions().find(
                        EntityKey.<RegionRecord>builder()
                                .set(REGION.WORLD_ID, reference.worldId())
                                .set(REGION.ID, reference.regionId())
                                .build()
                )
                .map(record -> new RegionImpl(record, context));
    }

    @Override
    public List<LocationSnapshot> getLocations() {
        return context.locations().getAll().stream()
                .map(record -> new LocationImpl(record, context))
                .map(record -> (LocationSnapshot) record)
                .toList();
    }

    @Override
    public Optional<LocationSnapshot> getLocation(LocationSnapshot.Reference reference) {
        return context.locations().find(
                        EntityKey.<LocationsRecord>builder()
                                .set(LOCATIONS.WORLD_ID, reference.worldId())
                                .set(LOCATIONS.ID, reference.id())
                                .build()
                )
                .map(record -> new LocationImpl(record, context));
    }

    @Override
    public List<EntrySnapshot> getEntries() {
        return context.entries().getAll().stream()
                .map(record -> new EntryImpl(record, context))
                .map(record -> (EntrySnapshot) record)
                .toList();
    }

    @Override
    public Optional<EntrySnapshot> getEntry(EntrySnapshot.Reference reference) {
        return context.entries().find(
                        EntityKey.<EntryRecord>builder()
                                .set(ENTRY.LOREBOOK_ID, reference.lorebookId())
                                .set(ENTRY.ENTRY_ID, reference.entryId())
                                .build()
                )
                .map(record -> new EntryImpl(record, context));
    }

    @Override
    public List<PromptSectionEntitySnapshot> getPromptSections() {
        return context.sections().getAll().stream()
                .map(record -> new PromptSectionEntityImpl(record, context))
                .map(record -> (PromptSectionEntitySnapshot) record)
                .toList();
    }

    @Override
    public Optional<PromptSectionEntitySnapshot> getPromptSection(PromptSectionEntitySnapshot.Reference reference) {
        return context.sections().find(
                        EntityKey.<PromptSectionRecord>builder()
                                .set(PROMPT_SECTION.PROMPT_ID, (short) reference.promptId())
                                .set(PROMPT_SECTION.SECTION_ID, (short) reference.sectionId())
                                .build()
                )
                .map(record -> new PromptSectionEntityImpl(record, context));
    }
}
