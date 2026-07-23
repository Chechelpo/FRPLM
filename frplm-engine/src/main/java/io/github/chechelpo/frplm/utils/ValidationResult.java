package io.github.chechelpo.frplm.utils;

import io.micrometer.common.lang.internal.Contract;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public sealed interface ValidationResult {
        @Contract(" -> new")
        static ValidationResult.@NonNull Success success(){
            return new Success();
        }
        static ValidationResult.Error error(String message){
            Objects.requireNonNull(message);
            return new ValidationResult.Error(message);
        }
        
        default boolean isSuccess(){
            return this instanceof ValidationResult.Success;
        }
        default boolean isFailure(){
            return this instanceof ValidationResult.Error;
        }
        
        default <X extends Throwable> ValidationResult ifFailureThrow(
                Function<String, ? extends X> exceptionFactory
        ) throws X {
            if (this instanceof Error(String message))
                throw exceptionFactory.apply(message);
            
            return this;
        }
        
        record Error(String message) implements ValidationResult{}
        record Success() implements ValidationResult {}
}