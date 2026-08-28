// src/index.ts
/**
 * Public entry point of the FRPLM host SDK.
 *
 * The SDK exposes:
 *  - the framework-agnostic domain layer (entities, DTOs, codecs)
 *  - the type-only host bindings consumed by extension panels
 *  - the custom-element tag registry (`FRPLM_COMPONENT_TAGS`)
 *  - a small HTTP client that normalises API paths, handles CSRF, and
 *    forwards non-EXPECTED errors to a host-registered handler.
 *
 * It contains no Vue components and no UI code. The only Vue usage is
 * a `reactive()` call inside the abstract `ABSEntity` base class to
 * make entity data reactive when consumed by Vue. Vue is therefore a
 * peer dependency.
 */

// Domain entities and their key/data types.
export * from "./domain/Characters.js";
export * from "./domain/Connection.js";
export * from "./domain/EntityTypes.js";
export * from "./domain/Lorebook.js";
export * from "./domain/Prompts.js";
export * from "./domain/Tag.js";
export * from "./domain/World.js";
export * from "./domain/Session.js";

// Core abstractions.
export * from "./core/ABSEntity.js";
export * from "./core/FieldMetadata.js";
export * from "./core/interfaces.js";
export * from "./core/queries.js";

// DTO / contract types.
export * from "./types/ChatCompletions.js";
export * from "./types/DTOs.js";
export * from "./types/Equatable.js";

// Pure utilities.
export * from "./utils/CommonFields.js";
export * from "./utils/CustomSet.js";
export * from "./utils/PromptThings.js";
export * from "./utils/ReferenceCodec.js";

// HTTP client and CSRF helpers.
export {
    ApiRequestError,
    Severity,
    apiClient,
    clearCsrfToken,
    fetchApi,
    initializeApiSecurity,
    logError,
    onGlobalError,
} from "./services/apiClient.js";
export type {
    ErrorResponse,
    FetchApiInit,
    GlobalErrorHandler,
} from "./services/apiClient.js";

// Config.
export {
    API_BASE,
    configureApiBase,
    getApiBase,
} from "./config.js";

// Component tag registry.
export {
    FRPLM_COMPONENT_TAGS,
} from "./components.js";
export type {
    FrplmComponentName,
    FrplmComponentRegistry,
    FrplmComponentTag,
} from "./components.js";

// Host bindings (type-only).
export type {
    FrplmExtensionBindings,
    FrplmHostBindings,
} from "./HostSdkBindings.js";
