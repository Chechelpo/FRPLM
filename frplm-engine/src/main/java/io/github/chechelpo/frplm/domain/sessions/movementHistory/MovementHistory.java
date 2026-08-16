package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class MovementHistory {
    public List<LocationsRecord> getVisitedLocationsOf(SessionCharactersRecord character){
        throw new UnsupportedAction("");
    }
}
