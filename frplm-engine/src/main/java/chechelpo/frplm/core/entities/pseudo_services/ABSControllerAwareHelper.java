package chechelpo.frplm.core.entities.pseudo_services;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Objects;

/**
 * Class used for centralizing initialization of entities.
 */
public abstract class ABSControllerAwareHelper<
        R extends TableRecord<R>,
        Service extends EntityService<R,?>,
        C extends EntityController<R, Service>
        > extends ABSHelper<R, Service> {
    protected final C controller;

    protected ABSControllerAwareHelper(Service service, C controller) {
        super(service);
        Objects.requireNonNull(controller);
        this.controller = controller;
    }
    protected <T> void register_field(
            @Nullable String dto_name,
            @NotNull TableField<R, T> column,
            @NotNull FieldInfo<?> info,
            T defaultValue
    ){
        super.register_field(column, info, defaultValue);
        if(dto_name != null) controller.registerPublicField(column, dto_name, info.format);
    }

    /**
     * Registers a field to be used by the controller.
     * @param dto_name the name of the attr, distinct from its SQL name (or not). This is the name that will be received in frontend.
     * @param column of the attribute
     * @param info metadata of field. For frontend.
     * @param <T> The actual field type
     */
    protected <T> void register_field(
            @Nullable String dto_name,
            @NotNull TableField<R, T> column,
            @NotNull FieldInfo<?> info
    ) {
        super.register_field(column, info);

        if (dto_name != null) controller.registerPublicField(column, dto_name, info.format);
    }

}
