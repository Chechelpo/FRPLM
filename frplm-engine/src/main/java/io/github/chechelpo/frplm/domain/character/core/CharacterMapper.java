package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.NewCharacterOrder;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public final class CharacterMapper extends ABSWireMapper<CharactersRecord, CharacterJSON, NewCharacterOrder> {
    private final EntityReaders readers;
    private final EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    public CharacterMapper(
            ObjectMapper mapper,
            EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper,
            EntityReaders readers
    ) {
        super(mapper, CharacterJSON.class, null);
        this.lorebookMapper = lorebookMapper;
        this.readers = readers;
    }

    @Override
    protected String getZipPath(CharacterJSON json) {
        throw new UnsupportedOperationException();
    }


    @Override
    @Contract("_,_ -> new")
    public CharacterJSON internalRecordFrom(@NonNull CharactersRecord record, @NonNull ZipBuilder zipBuilder) {
        return new CharacterJSON(
                record.getName(),
                record.getDescription(),
                record.getCanBeUser(),
                record.getIsArchetype(),
                record.getWelcomeMessage(),
                lorebookMapper.jsonRecordFrom(
                        readers.lorebooks().require(EntityKey.of(LOREBOOKS.ID,record.getLorebookId())),
                        zipBuilder
                )
        );
    }

    @Contract("_ -> new")
    @Override
    public @NonNull NewCharacterOrder internalOrderFrom(@NonNull CharacterJSON json){
        return new NewCharacterOrder(
                EntityDataPayload.<CharactersRecord>builder()
                        .set(CHARACTERS.NAME, json.name())
                        .set(CHARACTERS.DESCRIPTION, json.description())
                        .set(CHARACTERS.CAN_BE_USER, json.can_be_user())
                        .set(CHARACTERS.IS_ARCHETYPE, json.is_archetype())
                        .set(CHARACTERS.WELCOME_MESSAGE, json.welcome_message())
                        .build()
                ,
                lorebookMapper.orderFrom(json.lorebook())
        );
    }
}
