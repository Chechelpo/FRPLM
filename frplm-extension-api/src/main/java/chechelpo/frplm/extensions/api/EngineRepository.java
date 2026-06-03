package chechelpo.frplm.extensions.api;

import chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import chechelpo.frplm.extensions.api.standalone.PromptSnapshot;
import chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public interface EngineRepository {
    @Unmodifiable
    @NotNull List<CharacterSnapshot> getCharacters();
    Optional<CharacterSnapshot> getCharacterWithName(String name);
    Optional<CharacterSnapshot> getCharacter(CharacterSnapshot.Reference reference);

    @Unmodifiable
    @NotNull List<ConnectionSnapshot> getConnections();
    Optional<ConnectionSnapshot> getConnectionWithName(String name);
    Optional<ConnectionSnapshot> getConnection(ConnectionSnapshot.Reference reference);

    @Unmodifiable
    @NotNull List<WorldSnapshot> getWorlds();
    Optional<WorldSnapshot> getWorldWithName(String name);
    Optional<WorldSnapshot> getWorld(WorldSnapshot.Reference reference);

    @Unmodifiable
    @NotNull List<PromptSnapshot> getPrompts();
    Optional<PromptSnapshot> getPromptWithName(String name);
    Optional<PromptSnapshot> getPrompt(PromptSnapshot.Reference reference);
}
