package chechelpo.frplm.domain.tags.core;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.TAGS;

@Component
final class TagFieldHelper extends ABSFieldInstantiationHelper<TagsRecord, TagStore,TagService, TagController> {
    TagFieldHelper(TagStore store, TagService service, TagController controller) {
        super(store, service, controller);
        register_field(
                "id",
                TAGS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
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
                        .setConstraints(StringConstraints.builder()
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
                        .setConstraints(StringConstraints.builder()
                                .setMaxLength(7)
                                .build()
                        )
                        .build()
        );
    }
}
