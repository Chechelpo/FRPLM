package io.github.chechelpo.frplm.core.entities.pseudo_services;

import io.github.chechelpo.frplm.exceptions.RuntimeDomainException;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.ExpectedField;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.UnknownFieldException;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;

public sealed interface FieldActionResult<R extends TableRecord<R>, D extends DataPayload<R>> {
    D payload();
    BiFunction<String, Severity, RuntimeDomainException> getDefaultExceptionConstructor();
    String debugString();
    String validatorMessage();

    default boolean isSuccess() {
        return this instanceof FieldActionResult.Success<R, D>;
    }
    default boolean isFailure() {
        return !isSuccess();
    }

    default Severity getDefaultSeverity(){
        return Severity.USER;
    }
    default String getDefaultCompoundMessage(@Nullable String whenDoing){
        return validatorMessage() + whenDoing + "\n" + debugString();
    }

    default Success<R, D> orElseThrow(String message, Severity severity){
        return (Success<R, D>) this
                .ifMissingFieldThrow(message, severity)
                .ifUnknownFieldThrow(message, severity)
                .ifWrongValueThrow(message, severity);
    }
    default Success<R, D> orElseThrow(String message){
        return (Success<R, D>) this
                .ifMissingFieldThrow(message)
                .ifUnknownFieldThrow(message)
                .ifWrongValueThrow(message);
    }
    default Success<R, D> orElseThrow(){
        return (Success<R, D>) this
                .ifMissingFieldThrow()
                .ifUnknownFieldThrow()
                .ifWrongValueThrow();
    }
    default Success<R, D> get(){
        if (this instanceof Success<R, D> success) return success;
        throw new IllegalStateException("Called get() on a failure " + debugString());
    }
    @Contract("_ -> new")
    static <R extends TableRecord<R>, D extends DataPayload<R>> @NonNull Success<R, D> success(D payload){
        return new Success<>("Successful validation",payload);
    }
    record Success<R extends TableRecord<R>, D extends DataPayload<R>>(String validatorMessage, D payload) implements FieldActionResult<R, D> {
        @Override
        public BiFunction<String, Severity, RuntimeDomainException> getDefaultExceptionConstructor() {
            throw new UnsupportedAction("This is a successful FieldValidationResult");
        }

        @Override
        public String debugString() {
            return "Successful field validation result for \n" + payload;
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Unknown field
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    default FieldActionResult<R, D> ifUnknownFieldThrow(){
        return ifUnknownFieldThrow(null, getDefaultSeverity());
    }
    default FieldActionResult<R, D> ifUnknownFieldThrow(String message){
        return ifUnknownFieldThrow(message, getDefaultSeverity());
    }
    default FieldActionResult<R, D> ifUnknownFieldThrow(String message, Severity severity){
        if (this instanceof UnknownField<R, D> unknownField){
            throw unknownField.getDefaultExceptionConstructor().apply(getDefaultCompoundMessage(message), severity);
        }

        return this;
    }

    @Contract("_, _ -> new")
    static <R extends TableRecord<R>, D extends DataPayload<R>> @NonNull UnknownField<R, D> unknownField(String message, TableField<R, ?> field, D payload){
        return new UnknownField<>(message, field, payload);
    }
    record UnknownField<R extends TableRecord<R>, D extends DataPayload<R>>(String validatorMessage, TableField<R, ?> field, D payload)
            implements FieldActionResult<R, D>
    {
        @Override
        public BiFunction<String, Severity, RuntimeDomainException> getDefaultExceptionConstructor() {
            return UnknownFieldException::new;
        }

        @Override
        public String debugString() {
            return "Unregistered field " + field.getName() + " from payload: \n" + payload.toString();
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Missing field
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    default FieldActionResult<R, D> ifMissingFieldThrow(){
        return ifMissingFieldThrow(null, getDefaultSeverity());
    }
    default FieldActionResult<R, D> ifMissingFieldThrow(String message){
        return ifMissingFieldThrow(message, getDefaultSeverity());
    }
    default FieldActionResult<R, D> ifMissingFieldThrow(String message, Severity severity){
        if (this instanceof MissingField<R, D> missingField){
            throw missingField.getDefaultExceptionConstructor().apply(getDefaultCompoundMessage(message), severity);
        }

        return this;
    }

    static <R extends TableRecord<R>, D extends DataPayload<R>> MissingField<R,D> missingField(
            String validatorMessage,
            TableField<R, ?> field,
            D payload
    ){
        return new MissingField<>(validatorMessage, field, payload);
    }
    record MissingField<R extends TableRecord<R>, D extends DataPayload<R>>(
            String validatorMessage, TableField<R, ?> field, D payload
    )
            implements FieldActionResult<R, D>
    {
        @Override
        public BiFunction<String, Severity, RuntimeDomainException> getDefaultExceptionConstructor() {
            return ExpectedField::new;
        }

        @Override
        public String debugString() {
            return "Payload is missing field " + field.getName() + ". Payload: \n" + payload.toString();
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Wrong value
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    default FieldActionResult<R, D> ifWrongValueThrow(){
        return ifWrongValueThrow(null, getDefaultSeverity());
    }
    default FieldActionResult<R, D> ifWrongValueThrow(String message){
        return ifWrongValueThrow(message, getDefaultSeverity());
    }
    default FieldActionResult<R, D> ifWrongValueThrow(String message, Severity severity){
        if (this instanceof WrongValue<R, D, ?> wrongValue){
            throw wrongValue.getDefaultExceptionConstructor().apply(getDefaultCompoundMessage(message), severity);
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    static <R extends TableRecord<R>, D extends DataPayload<R>, T> WrongValue<R, D, T> wrongValue(
            String validatorMessage,
            TableField<R, ?> field,
            Object wrongValue,
            D payload
    ){
        return new WrongValue<>(validatorMessage, (TableField<R,T>) field, (T) wrongValue, payload);
    }
    record WrongValue<R extends TableRecord<R>, D extends DataPayload<R>, T>(
            String validatorMessage,
            TableField<R, T> field, T wrongValue,
            D payload
    ) implements FieldActionResult<R, D>
    {
        @Override
        public BiFunction<String, Severity, RuntimeDomainException> getDefaultExceptionConstructor() {
            return InvalidValue::new;
        }

        @Override
        public String debugString() {
            return "Invalid value " + wrongValue + " found in field " + field.getName();
        }
    }
}
