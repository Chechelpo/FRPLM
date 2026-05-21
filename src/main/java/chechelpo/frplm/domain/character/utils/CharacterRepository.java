package chechelpo.frplm.domain.character.utils;

import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.springframework.stereotype.Component;

@Component
final class CharacterRepository extends EntityRepository<CharactersRecord, CharacterService> {
    CharacterRepository(CharacterService service) {
        super(service);
    }
}
