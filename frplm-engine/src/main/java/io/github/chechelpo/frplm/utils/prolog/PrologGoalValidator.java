package io.github.chechelpo.frplm.utils.prolog;

import org.springframework.stereotype.Component;

@FunctionalInterface
public interface PrologGoalValidator {
    /**
     * Validates an assertion such as parent(alice, Y), in_location(Jonathan, townSquare)
     * <p>
     *     First validates if the syntax is correct, then if the function name exists. Then validates that the amount of arguments
     *     is correct. DOES NOT VALIDATE EXISTENCE OF
     * </p>
     */
    void validateGoal(String source);
}
