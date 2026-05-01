package chechelpo.demo.frameworks.entities.microservices;

import chechelpo.demo.frameworks.entities.fields.FieldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jooq.TableRecord;

/**
 * Class used for centralizing initialization of entities.
 */
public class ABSFieldInstantiationHelper<
        R extends TableRecord<R>,
        Store extends ABSEntityStore<R>,
        Service extends ABSEntityService<R,Store>,
        C extends ABSEntityController<R, Service>
        > {
    protected final Store store;
    protected final Service service;
    protected final C controller;

    protected ABSFieldInstantiationHelper(Store store, Service service, C controller) {
        this.store = store;
        this.service = service;
        this.controller = controller;
    }

    protected <T> void register_field(
            @Nullable String dto_name,
            @NotNull TableField<R, T> column,
            @NotNull FieldInfo.FieldInfoBuilder<?> infoBuilder
    ) {
        this.register_field(dto_name, column, infoBuilder.build());
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
        service.registerField(column, info.require, info.constraints);
        controller.registerPublicField(column, dto_name, info.format);
    }

}
