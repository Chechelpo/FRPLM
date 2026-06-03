package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;

public class LocationImpl extends StandaloneEntity<LocationsRecord> implements LocationSnapshot {
    public LocationImpl(LocationsRecord record, ExtensionContext context) {
        super(record, context);
    }

    public LocationsRecord getRecord() {
        return record;
    }

    @Override
    public Reference reference() {
        return new LocationSnapshot.Reference(this.record.getWorldId(), this.record.getId());
    }

    @Override
    public String getName(){
        return record.getName();
    }

    @Override
    public LocationImpl @NotNull [] getNeighbours() {
        return context.edges().getNeighboursOf(this.record).stream()
                .map(record -> new LocationImpl(record, this.context))
                .toArray(LocationImpl[]::new);
    }
}
