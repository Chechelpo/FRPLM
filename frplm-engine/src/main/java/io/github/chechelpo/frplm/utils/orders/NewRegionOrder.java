package io.github.chechelpo.frplm.utils.orders;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;

import java.util.Optional;
import java.util.Set;

public record NewRegionOrder(
        String zipPath,
        EntityDataPayload<RegionRecord> payload,
        NewLorebookOrder lorebookOrder,
        String parentRegionName
) implements CreationOrder<RegionRecord> {

    @Override
    public Optional<String> getZipPath() {
        return Optional.ofNullable(zipPath);
    }

}
