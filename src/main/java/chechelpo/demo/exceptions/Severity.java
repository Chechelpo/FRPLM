package chechelpo.demo.exceptions;

public enum Severity {
    /** Exceptions that signal things like forbidden fields or missing assets (that are not yet uploaded) */
    EXPECTED,
    /** Exception caused by user input. Expected by the system, functions as a warning */
    USER,
    /** Error from a system call. Unexpected, probably fatal. */
    SYSTEM
}
