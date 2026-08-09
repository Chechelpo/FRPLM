package io.github.chechelpo.frplm.utils.prolog.prolog_entity_translators;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.prolog.arguments.PrologArgumentType;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;

@Component
public class RegionEntityTranslator implements PrologEntityTranslator {

    private final EntityTranslator entityTranslator;
    private final WorldService worldService;
    private final RegionService regionService;

    public RegionEntityTranslator(EntityTranslator entityTranslator, WorldService worldService, RegionService regionService) {
        this.entityTranslator = entityTranslator;
        this.worldService = worldService;
        this.regionService = regionService;
    }

    @PostConstruct
    void setUp() {
        entityTranslator.register(PrologArgumentType.REGION, this);
    }

    @Override
    public Optional<String> getIdOfRepresentation(String argumentName) {
        io.github.chechelpo.frplm.extensions.api.utils.QualifiedNames.TwoParts parts = io.github.chechelpo.frplm.extensions.api.utils.QualifiedNames.splitTwo(argumentName);
        WorldsRecord world = worldService.getOneMatching(
                EntityDataPayload.of(WORLDS.NAME, parts.first())
        ).resolve();

        RegionRecord region = regionService.getOneMatching(
                EntityDataPayload.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, world.getId())
                        .set(REGION.NAME, parts.second())
                        .build()
        ).resolve();

        return Optional.of(new RegionSnapshot.Reference(region.getWorldId(), region.getId()).encode());
    }

    @Override
    public Optional<String> getQualifiedName(String id) {
        RegionSnapshot.Reference reference = RegionSnapshot.Reference.fromString(id);
        return regionService.find(EntityKey.<RegionRecord>builder()
                .set(REGION.WORLD_ID, reference.worldId())
                .set(REGION.ID, reference.regionId())
                .build()
        ).map(record -> io.github.chechelpo.frplm.extensions.api.utils.QualifiedNames.qualify(
                        worldService.find(EntityKey.of(WORLDS.ID, record.getWorldId())).orElseThrow(Severity.SYSTEM).getName(),
                        record.getName()
                )
        );
    }
}
