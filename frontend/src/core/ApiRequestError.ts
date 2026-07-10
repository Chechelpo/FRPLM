import type { ErrorResponse } from "./GlobalError";

export class ApiRequestError extends Error {
  readonly details: ErrorResponse;
  readonly response?: Response;

  constructor(
    details: ErrorResponse,
    response?: Response,
    options?: ErrorOptions,
  ) {
    super(details.message, options);

    this.name = "ApiRequestError";
    this.details = details;
    this.response = response;
  }
}