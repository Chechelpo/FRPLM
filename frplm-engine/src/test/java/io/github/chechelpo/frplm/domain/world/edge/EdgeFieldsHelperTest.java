package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import org.junit.jupiter.api.Test;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;
import static org.junit.jupiter.api.Assertions.*;

class EdgeFieldsHelperTest {

    @Test
    void linkingToSameLocationIdThrows(){
        EdgeFieldsHelper validator = new EdgeFieldsHelper();

        assertThrows(
                InvalidValue.class,
                () -> validator.validateCustom(
                        EntityDataPayload.<LocationEdgesRecord>builder()
                                .set(LOCATION_EDGES.WORLD_ID, 1)
                                .set(LOCATION_EDGES.FROM_LOCATION_ID, 1)
                                .set(LOCATION_EDGES.TO_LOCATION_ID, 1)
                                .build()
                )
        );
    }
}