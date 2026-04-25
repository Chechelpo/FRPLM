
/*
Matches frontend error response:
private record ErrorResponse(
    int status,
    String type,
    String message,
    String path,
    Severity severity) {}
*/
export type ErrorResponse = {
    status: number;
    type: string;
    message: string;
    path: string;
    severity: Severity;
}

enum Severity {
    EXPECTED = "EXPECTED",
    USER = "USER",
    SYSTEM = "SYSTEM",
}

export function log_error(prefix:string, err: ErrorResponse): void {
    if (err === null) {
        console.error("Empty error response when handling error. Prefix: " + prefix);
    }

    const content = prefix + err.message;
    switch (err.severity) {
        case Severity.EXPECTED: console.trace(content);
        case Severity.USER: console.info(content);
        case Severity.SYSTEM: console.error(content);
    }
}

