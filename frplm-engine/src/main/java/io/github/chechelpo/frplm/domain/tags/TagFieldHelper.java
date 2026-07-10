package io.github.chechelpo.frplm.domain.tags;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.TAGS;

@Component
final class TagFieldHelper extends ABSControllerAwareHelper<TagsRecord, TagService, TagController> {
    TagFieldHelper(TagService service, TagController controller) {
        super(service, controller);
        register_field(
                "id",
                TAGS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .build()
        );
        register_field(
                "name",
                TAGS.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(255)
                                .setMinLength(0)
                                .build()
                        )
                        .require()
                        .build()
        );
        register_field(
                "color",
                TAGS.COLOR,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(7)
                                .build()
                        )
                        .build()
        );
    }
}
