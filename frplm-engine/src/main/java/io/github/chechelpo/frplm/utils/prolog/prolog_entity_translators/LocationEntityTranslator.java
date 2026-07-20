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

        WorldsRecord world = worldService.getOneMatching(WORLDS.NAME, parts.first())
                .ifEmptyThrow(empty -> new EntityNotFound(
                        "Couldn't find world with name " + parts.first(),
                        Severity.SYSTEM
                ))
                .resolve();

        RegionRecord region = regionService.getOneMatching(
                        EntityDataPayload.<RegionRecord>builder()
                                .set(REGION.WORLD_ID, world.getId())
                                .set(REGION.NAME, parts.second())
                                .build()
                )
                .ifEmptyThrow(emptyResult -> new EntityNotFound(
                        "Couldn't find parent region with name " + parts.second(),
                        Severity.SYSTEM
                ))
                .ifMoreThanOneThrow(target -> new UnexpectedException(
                        "Multiple regions match name %s, got %s results for query: \n %s".formatted(parts.second(), target.matchCount(), target.target()),
                        Severity.SYSTEM)
                )
                .resolve();

        LocationsRecord location = locationsService.getOneMatching(
                        EntityDataPayload.<LocationsRecord>builder()
                                .set(LOCATIONS.WORLD_ID, world.getId())
                                .set(LOCATIONS.REGION_ID, region.getId())
                                .set(LOCATIONS.NAME, parts.third())
                                .build()
                ).resolve();

        return Optional.of(
                new LocationSnapshot.Reference(location.getWorldId(), location.getId()).encode()
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
