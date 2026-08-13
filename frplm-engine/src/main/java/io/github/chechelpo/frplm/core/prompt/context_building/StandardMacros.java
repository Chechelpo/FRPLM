package io.github.chechelpo.frplm.core.prompt.context_building;

import io.github.chechelpo.frplm.core.prompt.PromptPhase;
import io.github.chechelpo.frplm.core.prompt.PromptPipelineSection;
import io.github.chechelpo.frplm.core.prompt.building.PromptOrchestrator;
import io.github.chechelpo.frplm.extensions.api.session.Session;
import io.github.chechelpo.frplm.extensions.api.session.SessionLocation;
import io.github.chechelpo.frplm.extensions.implementations.session.SessionImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public final class StandardMacros implements PromptPipelineSection {
    @Override
    public PromptPhase requestPhase() {
        return PromptPhase.CONTEXT_BUILDING;
    }

    @Override
    public void run(@NonNull PromptOrchestrator orchestrator) {
        orchestrator.getSession()
                .ifPresent(
                        session -> run(session, orchestrator)
                );
    }

    private void run(Session session, PromptOrchestrator orchestrator){
        orchestrator.appendAtMacro("user", session.getUserCharacter().getName());
        injectFormattedCharacters(session, orchestrator);

        orchestrator.appendAtMacro("location", session.getUserCharacter().getCurrentLocation().getName());
        orchestrator.appendAtMacro("region", session.getUserCharacter().getCurrentLocation().getParentRegion().getName());
        orchestrator.appendAtMacro("world", session.getWorld().getName());
    }

    private void injectFormattedCharacters(Session session, PromptOrchestrator orchestrator){
        SessionLocation location = session.getUserCharacter().getCurrentLocation();
        StringBuilder builder = new StringBuilder();

        builder.append("<characters in %s>".formatted(location.getName()));
        location.getCharactersHere().forEach(character -> builder.append(
                getFormattedStringAndDescription(character.getName(), character.getDescription())
        ));
        builder.append("</characters in %s>".formatted(location.getName()));

        orchestrator.appendAtMacro("formatted_characters", builder.toString());
    }

    private String getFormattedStringAndDescription(String name, String description){
        return """
                <%s>
                <name> %s </name>
                <description>
                %s
                </description>
                <extra> {{extra}} </extra>
                <%s>
                """.formatted(
                        name, name, description, name
        );
    }
}
