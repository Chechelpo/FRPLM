package io.github.chechelpo.frplm.utils.prolog.prolog_entity_translators;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class LocationEntityTranslator implements PrologEntityTranslator {

    private final WorldService worldService;
    private final LocationsService locationsService;
    private final RegionService regionService;

    LocationEntityTranslator(WorldService worldService, LocationsService locationsService, RegionService regionService) {
        this.worldService = worldService;
        this.locationsService = locationsService;
        this.regionService = regionService;
    }

    @Override
    public @NonNull Optional<String> getIdOfRepresentation(String argumentName) {
        QualifiedNames.ThreeParts parts = QualifiedNames.splitThree(argumentName);
        Result<WorldsRecord> matchingWorlds = worldService.getMatching(WORLDS.NAME, parts.first());
        if (matchingWorlds.size() != 1)
            throw new UnexpectedException("Expected to get one world with name %s. Instead got %s".formatted(parts.first(), matchingWorlds),
                    Severity.SYSTEM
            );

        Result<RegionRecord> regions = regionService.getMatching(
                EntityDataPayload.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, matchingWorlds.getFirst().getId())
                        .set(REGION.NAME, parts.second())
                        .build()
        );
        if (regions.size() != 1)
            throw new UnexpectedException("Expected to get a single region with name %s, instead got %s".formatted(parts.second(), regions),
                    Severity.SYSTEM
            );

        Result<LocationsRecord> locations = locationsService.getMatching(
                EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.ID, matchingWorlds.getFirst().getId())
                        .set(LOCATIONS.REGION_ID, regions.getFirst().getId())
                        .set(LOCATIONS.NAME, parts.third())
                        .build()
        );
        if (locations.size() != 1)
            throw new UnexpectedException(
                    "Expected to get one location with name %s. Instead got %s".formatted(parts.first(), locations),
                    Severity.SYSTEM
            );

        return Optional.of(
                new LocationSnapshot.Reference(locations.getFirst().getId(), locations.getFirst().getId()).encode()
        );
    }

    /**
     * @return {worldName}.{regionName}.{locationName}
     */
    @Override
    public Optional<String> getQualifiedName(String id) {
        LocationSnapshot.Reference reference = LocationSnapshot.Reference.fromString(id);
        return locationsService.find(
                EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, reference.worldId())
                        .set(LOCATIONS.ID, reference.id())
                        .build()
        ).map(record -> QualifiedNames.qualify(
                worldService.find(EntityKey.of(WORLDS.ID, reference.id()))
                        .orElseThrow(() -> new EntityNotFound("Couldn't find world when getting qualified name for " + record, Severity.SYSTEM))
                        .getName(),
                regionService.find(EntityKey.<RegionRecord>builder()
                                .set(REGION.WORLD_ID, reference.worldId())
                                .set(REGION.ID, record.getRegionId())
                                .build()
                        ).orElseThrow(() -> new EntityNotFound("Couldn't find region for " + record, Severity.SYSTEM))
                        .getName(),
                record.getName()
        ));
    }
}
