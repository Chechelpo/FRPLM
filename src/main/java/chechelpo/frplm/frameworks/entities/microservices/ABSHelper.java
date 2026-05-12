package chechelpo.frplm.frameworks.entities.microservices;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;

public abstract class ABSHelper<
        R extends TableRecord<R>,
        Service extends ABSEntityService<R,?>
        > {
    protected final Service service;

    public ABSHelper(Service service) {
        this.service = service;
    }

    protected <T> void register_field(
            @NotNull TableField<R, T> column,
            @NotNull FieldInfo.FieldInfoBuilder<?> infoBuilder
    ) {
        this.register_field(column, infoBuilder.build());
    }

    /**
     * Registers a field to be used by the service.
     * @param column of the attribute
     * @param info metadata of field. For frontend.
     * @param <T> The actual field type
     */
    protected <T> void register_field(
            @NotNull TableField<R, T> column,
            @NotNull FieldInfo<?> info
    ) {
        service.registerField(column, info.require, info.constraints);
    }
}
