package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Objects;

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
        Objects.requireNonNull(column, "Column field is null");
        Objects.requireNonNull(info, "Field info is null");
        service.registerField(column, info.require, info.constraints);
    }
    protected <T> void register_field(
            @NotNull TableField<R, T> column,
            @NotNull FieldInfo<?> info,
            T defaultValue
    ){
        if (info.constraints.validateConstraint(column, defaultValue, false).isPresent())
            throw new IllegalArgumentException("Default value does not pass constraint");

        service.registerField(column, info.require, info.constraints, defaultValue);
    }

    protected <T> void register_field(FieldBuilder<T> builder){
        Objects.requireNonNull(builder, "Builder is null");
        this.register_field(builder.column, builder.info, builder.defaultValue);
    }

    public <T> FieldBuilder<T> register_field(TableField<R, T> column){
        return new FieldBuilder<>(column);
    }

    public class FieldBuilder<T> {
        private final TableField<R,T> column;
        FieldInfo<?> info = null;
        T defaultValue = null;

        private FieldBuilder(TableField<R,T> column){
            this.column = column;
        }

        public FieldBuilder<T> setInfo(FieldInfo<?> info){
            this.info = info;
            return this;
        }

        public FieldBuilder<T> withDefaultValue(T defaultValue){
            this.defaultValue = defaultValue;
            return this;
        }
    }
}
