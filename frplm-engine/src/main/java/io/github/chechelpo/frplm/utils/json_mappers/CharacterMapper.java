package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewCharacterOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;

@Component
public class CharacterMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final LorebookMapper lorebookMapper;
    private final LorebookService lorebookService;

    public CharacterMapper(LorebookMapper lorebookMapper, LorebookService lorebookService) {
        this.lorebookMapper = lorebookMapper;
        this.lorebookService = lorebookService;
    }

    private record CharacterJSON(
            String name,
            String description,
            boolean can_be_user,
            boolean is_archetype,
            String welcome_message,
            JsonNode lorebook
    ){}

    public JsonNode jsonFrom(@NonNull CharactersRecord record){
        return MAPPER.valueToTree(new CharacterJSON(
                record.getName(),
                record.getDescription(),
                record.getCanBeUser(),
                record.getIsArchetype(),
                record.getWelcomeMessage(),
                lorebookMapper.jsonFrom(lorebookService.getLorebookOf(record))
        ));
    }


    public NewCharacterOrder fromJson(JsonNode node){
        CharacterJSON json = MAPPER.treeToValue(node, CharacterJSON.class);
        return new NewCharacterOrder(
                EntityDataPayload.<CharactersRecord>builder()
                        .set(CHARACTERS.NAME, json.name)
                        .set(CHARACTERS.DESCRIPTION, json.description)
                        .set(CHARACTERS.CAN_BE_USER, json.can_be_user)
                        .set(CHARACTERS.IS_ARCHETYPE, json.is_archetype)
                        .set(CHARACTERS.WELCOME_MESSAGE, json.welcome_message)
                        .build()
                ,
                lorebookMapper.orderFrom(json.lorebook)
        );
    }
}
