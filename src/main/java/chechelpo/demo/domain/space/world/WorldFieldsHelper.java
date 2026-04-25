package chechelpo.demo.domain.space.world;

import chechelpo.demo.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.demo.jooq.generated.tables.records.WorldsRecord;
import chechelpo.demo.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.demo.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.demo.frameworks.entities.fields.format.StringFormat;
import chechelpo.demo.frameworks.entities.fields.FieldInfo;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

import static chechelpo.demo.jooq.generated.Tables.WORLDS;

@Component
public final class WorldFieldsHelper extends ABSFieldInstantiationHelper<
        WorldsRecord,
        WorldStore,
        WorldService,
        WorldController
        > {
    WorldFieldsHelper(
            WorldStore store,
            WorldService service,
            WorldController controller
    ) {
        super(store, service, controller);

        register_field(
                "id",
                WORLDS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.INTEGER)
                                        .key()
                                        .readOnly()
                                        .build()
                        )
                        .build()
        );

        register_field(
                "name",
                WORLDS.NAME,
                FieldInfo.stringField()
                        .setFormat(
                                StringFormat.builder()
                                        .setType(StringFormat.Types.SHORT_TEXT)
                                        .setInfo("World name, must be unique")
                                        .build()
                        )
                        .setConstraints(
                                StringConstraints.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .build()
        );

        register_field(
                "lorebook_id",
                WORLDS.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
    }
}
