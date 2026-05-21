package chechelpo.frplm.domain.world.location.utils;

import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.Entity;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.Contract;


public final class LocationEntity extends Entity<LocationsRecord, LocationRepository> {
    LocationEntity(EntityKey<LocationsRecord> key, LocationRepository repository) {
        super(key, repository);
    }
    public String getFormattedInfo(){
        throw new RuntimeException("aa");
    }
    @Contract(mutates = "param1")
    private void appendNeighboursInfo(StringBuilder builder){
    }
}
