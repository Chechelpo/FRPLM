package chechelpo.frplm.extensions;

import chechelpo.frplm.extensions.api.EngineRepository;
import chechelpo.frplm.extensions.api.standalone.*;
import chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import chechelpo.frplm.extensions.implementations.standalone.*;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class ExtensionRepository implements EngineRepository {
    private final ExtensionContext context;

    public ExtensionRepository(ExtensionContext context) {
        this.context = context;
    }

    ExtensionContext getContext() {
        return context;
    }


    @Override
    public <T> Optional<?> get(@NotNull Class<T> type, @NotNull String reference) {
        Class<?> rawType = type;

        if ( rawType == ConnectionSnapshot.class )
            return this.getConnection(ConnectionSnapshot.Reference.fromString(reference));
        if ( rawType == CharacterSnapshot.class )
            return this.getCharacter(CharacterSnapshot.Reference.fromString(reference));
        if ( rawType == PromptSnapshot.class )
            return this.getPrompt(PromptSnapshot.Reference.fromString(reference));

        return Optional.empty();
    }

    @Override
    public @Unmodifiable @NotNull List<CharacterSnapshot> getCharacters() {
        return context.characters().getAll().stream()
                .map(record -> new CharacterImpl(record, context))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<CharacterSnapshot> getCharacterWithName(String name) {
        return context.characters().getCharacterWith(name).map(record -> new CharacterImpl(record, context));
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
                .map(record -> new ConnectionImpl(record, context)).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<ConnectionSnapshot> getConnectionWithName(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<ConnectionSnapshot> getConnection(ConnectionSnapshot.Reference reference) {
        return context.connections().find(EntityKey.of(LLM_CONNECTION.ID, reference.id()))
                .map(record -> new ConnectionImpl(record, context));
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
    public Optional<LorebookSnapshot> getLorebook(LorebookSnapshot.Reference reference) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public @Unmodifiable @NotNull List<PromptSnapshot> getPrompts() {
        return context.templates().getAll().stream()
                .map(record -> new PromptImpl(record, context))
                .collect(Collectors.toUnmodifiableList());
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
        ).map(record -> new PromptImpl(record, context));
    }
}
