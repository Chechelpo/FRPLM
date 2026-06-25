package context_injector;

import chechelpo.frplm.extensions.api.activation.PrePromptGeneration;
import chechelpo.frplm.extensions.api.annotations.FrplmExtension;
import chechelpo.frplm.extensions.api.session.Session;
import chechelpo.frplm.extensions.api.session.SessionCharacter;
import chechelpo.frplm.extensions.api.session.SessionLocation;
import chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import chechelpo.frplm.extensions.api.types.Extension;
import chechelpo.frplm.extensions.api.utils.PromptBuilder;
import chechelpo.frplm.openai_compatible.ChatCompletionMessage;

import java.util.Arrays;

@FrplmExtension
public class ContextInjector extends Extension implements PrePromptGeneration {
    public ContextInjector() {
        super(
                "context-injector",
                "Context injector",
                """
                        Injects a section consisting of the current location and other information, right at the end.
                        """,
                null
        );
    }

    @Override
    public void run(Session ofSession, PromptBuilder prompt) {
        SessionLocation currentLocation = ofSession.getUserCharacter().getCurrentLocation();
        prompt.insertAt(
                2,
                ChatCompletionMessage.system("""
                                   [
                                   User character: %s ;
                                   Current location: %s ;
                                   Present characters (they may or may not be present in narration): %s ;
                                   Neighbouring locations: %s ;
                                   ]
                                   Continue with your response.
                                """.formatted(
                                ofSession.getUserCharacter().getName(),
                                ofSession.getUserCharacter().getCurrentLocation().getName(),
                                Arrays.stream(currentLocation.getCharactersHere())
                                        .map(SessionCharacter::getName)
                                        .toList(),
                                Arrays.stream(currentLocation.getSessionNeighbours())
                                        .map(LocationSnapshot::getName)
                                        .toList()
                        )
                )
        );
    }
}
