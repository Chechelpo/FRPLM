package io.github.chechelpo.frplm.utils.orders;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record NewLocationOrder (
    EntityDataPayload<LocationsRecord> payload,
    String parentRegionName,
    NewLorebookOrder lorebookOrder,
    List<NewCharacterOrder> charactersStartingHere
) implements CreationOrder<LocationsRecord> {

    @Contract(pure = true)
    @Override
    public @NonNull Optional<String> getZipPath() {
        return Optional.empty();
    }

}
