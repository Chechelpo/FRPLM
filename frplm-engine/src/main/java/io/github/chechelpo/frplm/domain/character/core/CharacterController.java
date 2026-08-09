package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.core.entities.fields.EntityDTO;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.CHARACTERS_URL;

@RestController
@RequestMapping(CHARACTERS_URL)
public final class CharacterController extends EntityController<
        CharactersRecord,
        CharacterService
        >
{

    CharacterController(CharacterService service, EntityControllerFieldValidator<CharactersRecord> validator) {
        super(service, validator);
    }


    @GetMapping("/startingAt")
    ResponseEntity<EntityDTO[]> getStartingAt(@RequestParam int worldId, @RequestParam int locationId) {
        return ResponseEntity.ok(
                wrapEntities(
                        service.getStartingAt(worldId, locationId)
                )
        );
    }

}
