package chechelpo.frplm.pipelines.prompts;

import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.*;
import chechelpo.frplm.pipelines.FullEngineContext;
import org.jetbrains.annotations.NotNull;

final class SessionContext {

    private SessionContext() {}

    static LorebooksRecord @NotNull [] gatherLorebooks(
            @NotNull LorebookService lorebookService,
            WorldsRecord world,
            LocationsRecord location,
            CharactersRecord @NotNull [] characters
    ) {
        LorebooksRecord[] lorebooks = new LorebooksRecord[characters.length + 2];
        try{
            lorebooks[0] = lorebookService.getLorebookOf(world);
            lorebooks[1] = lorebookService.getLorebookOf(location);

            for (int i = 0; i < characters.length; i++) {
                lorebooks[i + 2] = lorebookService.getLorebookOf(characters[i]);
            }
        } catch (EntityNotFound e) {
            throw new RuntimeException(e);
        }


        return lorebooks;
    }

    public static @NotNull PromptRenderContext getPromptRenderContext(
            SessionsRecord session,
            @NotNull FullEngineContext engine
    ) {
        CharactersRecord userCharacter = engine.characters().getUserCharacter(session);
        WorldsRecord world;
        LocationsRecord currentLocation;
        PromptTemplateRecord promptTemplate;
        try{
            currentLocation = engine.currentLocations().getLocationOf(userCharacter, session);
            promptTemplate = engine.templates().getOf(session)
                    .orElseThrow(() -> new RuntimeException("Template not configured"));
            world = engine.worlds().getWorldOf(session);
        } catch (EntityNotFound e) {
            throw new RuntimeException(e);
        }

        return new PromptRenderContext(
                userCharacter,
                promptTemplate,
                currentLocation,
                gatherLorebooks(
                        engine.lorebooks(),
                        world,
                        currentLocation,
                        engine.currentLocations().getAtLocation(currentLocation, session)
                        )
        );
    }
}
