package io.github.chechelpo.frplm.core.prompt.context_building;

import io.github.chechelpo.frplm.core.prompt.PromptPhase;
import io.github.chechelpo.frplm.core.prompt.PromptPipelineSection;
import io.github.chechelpo.frplm.core.prompt.building.PromptOrchestrator;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import io.github.chechelpo.frplm.extensions.api.session.SessionLocation;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
final class LorebookGathering implements PromptPipelineSection {
    LorebookGathering(){}

    @Override
    public PromptPhase requestPhase() {
        return PromptPhase.CONTEXT_BUILDING;
    }

    @Override
    public void run(PromptOrchestrator orchestrator) {
        orchestrator.getSession()
                .ifPresent(session -> run(session, orchestrator));
    }

    private void run(Session session, @NonNull PromptOrchestrator orchestrator) {
        addWorldLorebook(session, orchestrator);

        SessionLocation currentLocation = session.getUserCharacter().getCurrentLocation();
        orchestrator.addLorebook(currentLocation.lorebook());

        addRegionsLorebooks(currentLocation.getParentRegion(), orchestrator);

        addPresentCharactersLorebooks(currentLocation, orchestrator);
    }

    void addWorldLorebook(Session session, PromptOrchestrator orchestrator){
        orchestrator.addLorebook(session.getWorld().lorebook());
    }

    void addPresentCharactersLorebooks(SessionLocation location, PromptOrchestrator orchestrator){
        location.getCharactersHere()
                .forEach(character -> {
                    orchestrator.addLorebook(character.sessionLorebook());
                    character.getPermanentCharacter()
                            .ifPresent(permaCharacter -> orchestrator.addLorebook(permaCharacter.lorebook()));
                });
    }


    void addRegionsLorebooks(@NonNull RegionSnapshot root, PromptOrchestrator orchestrator){
        orchestrator.addLorebook(root.lorebook());

        Optional<RegionSnapshot> next = root.parent();
        while (next.isPresent()) {
            orchestrator.addLorebook(next.get().lorebook());
            next = next.get().parent();
        }
    }
}
