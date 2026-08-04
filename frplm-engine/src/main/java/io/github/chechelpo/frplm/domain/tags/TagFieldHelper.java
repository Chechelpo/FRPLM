package io.github.chechelpo.frplm.domain.tags;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.TAGS;

@Component
final class TagFieldHelper
        extends EntityControllerFieldValidator<TagsRecord> {

    TagFieldHelper() {
        super(EntityConfigs.Types.TAGS);
    }

    @Override
    protected List<DTOField<TagsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(TAGS.ID, "id"),
                DTOField.of(TAGS.NAME, "name"),
                DTOField.of(TAGS.COLOR, "color")
        );
    }

    @Override
    protected List<FieldInfo<TagsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(TAGS.ID)
                        .key()
                        .build(),

                FieldInfo.builder(TAGS.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMinLength(0)
                                        .setMaxLength(255)
                        )
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(TAGS.COLOR)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(7)
                        )
                        .build()
        );
    }
}