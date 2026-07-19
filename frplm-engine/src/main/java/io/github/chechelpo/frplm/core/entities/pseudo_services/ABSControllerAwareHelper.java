package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.ArrayList;
import java.util.List;
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

    List<ControllerFieldBuilder<?>> controllerFieldBuilders = new ArrayList<>();

    protected ABSControllerAwareHelper(Service service, C controller) {
        super(service);
        Objects.requireNonNull(controller);
        this.controller = controller;
    }
    @PostConstruct
    void registerControllerFields(){
        controllerFieldBuilders.forEach(
                builder -> {
                    register_field(builder);
                    Objects.requireNonNull(builder.dtoName, "Dto name is null for field " + builder.column);
                    controller.registerPublicField(builder.column, builder.dtoName, builder.info.format);
                }
        );
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

    protected <T> ControllerFieldBuilder<T> registerControllerField(TableField<R,T> column){
        ControllerFieldBuilder<T> builder =  new ControllerFieldBuilder<>(column);
        controllerFieldBuilders.add(builder);
        return builder;
    }

    public class ControllerFieldBuilder<T> extends FieldBuilder<T> {
        String dtoName = null;
        public ControllerFieldBuilder(TableField<R,T> column){
            super(column);
        }

        public ControllerFieldBuilder<T> setDtoName(String name){
            this.dtoName = name;
            return this;
        }
    }
}
