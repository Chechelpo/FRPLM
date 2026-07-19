package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.Constraint;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class ABSHelper<
        R extends TableRecord<R>,
        Service extends EntityService<R,?>
        > {
    protected final Service service;
    private List<FieldBuilder<?>> builders = new ArrayList<>(10);

    public ABSHelper(Service service) {
        this.service = service;
    }

    @PostConstruct
    private void registerBuilders(){
        builders.forEach(this::register_field);
        builders = null;
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
        Objects.requireNonNull(column, "Column field is null");
        Objects.requireNonNull(info, "Field info is null");
        service.registerField(column, info.require, info.constraints);
    }
    protected <T> void register_field(
            @NotNull TableField<R, T> column,
            @NotNull FieldInfo<?> info,
            T defaultValue
    ){
        Objects.requireNonNull(info, "Field info is null");

        service.registerField(column, info.require, info.constraints, defaultValue);
    }

    protected <T> void register_field(FieldBuilder<T> builder){
        Objects.requireNonNull(builder, "Builder is null");

        if (builder.assignedDefaultValue) this.register_field(builder.column, builder.info, builder.defaultValue);
        else this.register_field(builder.column, builder.info);
    }

    public <T> FieldBuilder<T> register_field(TableField<R, T> column){
        FieldBuilder<T> fieldBuilder = new FieldBuilder<>(column);
        builders.add(fieldBuilder);
        return fieldBuilder;
    }

    public class FieldBuilder<T> {
        protected final TableField<R,T> column;
        protected FieldInfo<?> info = null;
        boolean assignedDefaultValue = false;
        protected T defaultValue = null;

        protected FieldBuilder(TableField<R,T> column){
            this.column = column;
        }

        public TableField<R,T> getColumn(){
            return this.column;
        }
        public FieldInfo<?> getInfo(){
            return info;
        }
        public T getDefaultValue() {
            return defaultValue;
        }

        public FieldBuilder<T> setInfo(FieldInfo<?> info){
            this.info = info;
            return this;
        }

        public FieldBuilder<T> withDefaultValue(T defaultValue){
            this.defaultValue = defaultValue;
            assignedDefaultValue = true;
            return this;
        }
    }
}
