package chechelpo.frplm.core.entities.pseudo_services;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;

public abstract class ABSHelper<
        R extends TableRecord<R>,
        Service extends EntityService<R,?>
        > {
    protected final Service service;

    public ABSHelper(Service service) {
        this.service = service;
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
    protected <T> void register_field(
            @NotNull TableField<R, T> column,
            @NotNull FieldInfo<?> info,
            T defaultValue
    ){
        service.registerField(column, info.require, info.constraints, defaultValue);
    }
}
