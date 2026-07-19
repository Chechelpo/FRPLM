package io.github.chechelpo.frplm.utils.prolog.prolog_entity_translators;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.prolog.arguments.PrologArgumentType;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import jakarta.annotation.PostConstruct;
import org.jooq.Result;
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
        QualifiedNames.TwoParts parts = QualifiedNames.splitTwo(argumentName);
        Result<WorldsRecord> worlds = worldService.getMatching(
                EntityDataPayload.of(WORLDS.NAME, parts.first())
        );
        if (worlds.size() != 1)
            throw new UnexpectedException("Got more worlds than expected", Severity.SYSTEM);
        Result<RegionRecord> regions = regionService.getMatching(
                EntityDataPayload.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, worlds.getFirst().getId())
                        .set(REGION.NAME, parts.second())
                        .build()
        );
        if (worlds.size() != 1)
            throw new UnexpectedException("Got more regions than expected", Severity.SYSTEM);

        return Optional.of(new RegionSnapshot.Reference(regions.getFirst().getWorldId(), regions.getFirst().getId()).encode());
    }

    @Override
    public Optional<String> getQualifiedName(String id) {
        RegionSnapshot.Reference reference = RegionSnapshot.Reference.fromString(id);
        return regionService.find(EntityKey.<RegionRecord>builder()
                .set(REGION.WORLD_ID, reference.worldId())
                .set(REGION.ID, reference.regionId())
                .build()
        ).map(record -> QualifiedNames.qualify(
                        worldService.find(EntityKey.of(WORLDS.ID, record.getWorldId())).orElseThrow().getName(),
                        record.getName()
                )
        );
    }
}
