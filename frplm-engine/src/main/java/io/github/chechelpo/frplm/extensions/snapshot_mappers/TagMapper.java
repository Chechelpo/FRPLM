package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.TagSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.TagImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TagsRecord;

import static io.github.chechelpo.frplm.jooq.generated.Tables.TAGS;

final class TagMapper extends ReferenceMapper<TagsRecord, TagSnapshot.Reference, TagSnapshot> {
    TagMapper(EntityReader<TagsRecord> reader) {
        super(
                TagSnapshot.class,
                TagSnapshot.Reference::fromString,
                TagImpl::new,
                reference -> EntityKey.of(TAGS.ID, reference.tagId()),
                reader
        );
    }

    @Override
    TagSnapshot.Reference getExampleReference() {
        return new TagSnapshot.Reference(1);
    }
}
