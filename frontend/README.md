# FRPLM frontend monorepo

This repository contains the FRPLM frontend split into three sibling
packages. Each one is a standalone npm package with its own
`package.json`, can be built and tested in isolation, and is owned
by a specific contributor (see the ownership table below).

```
<root>/
├── sdk/   # @frplm/host-sdk  — framework-agnostic domain layer
├── ui/    # @frplm/ui       — Vue 3 primitive editor components
└── app/   # @frplm/frontend — the host Vue 3 application
```

## Who owns what

| Package           | Owned by      | Reason                                                                              |
|-------------------|---------------|-------------------------------------------------------------------------------------|
| `@frplm/host-sdk` | Project owner | **Stable contract.** Every extension panel imports from it. Breaking changes = major version bump and must be coordinated with extension authors. |
| `@frplm/ui`       | AI agents     | Styled component library. Visual contract; can iterate freely without breaking the SDK. |
| `@frplm/frontend` | AI agents     | The host SPA. Private — extensions never import from it. |

> If you are an agent working on this repo: **stay in your lane**.
> The SDK has a long review cycle because every extension breaks when
> it moves. The UI and the app can be edited freely.

## What lives where

### `sdk/` — `@frplm/host-sdk`

Framework-agnostic domain layer. **No Vue components, no UI code.**

- `src/domain/` — entities: `Character`, `World`, `Region`,
  `Location`, `Lorebook`, `Entry`, `Tag`, `LLMConnection`,
  `PromptTemplate`, `PromptSection`, `Message`, `Session`.
- `src/core/` — `ABSEntity` base class, `FieldMetadata`, `queries`,
  `interfaces`.
- `src/types/` — DTOs and the chat-completions payload.
- `src/utils/` — `ReferenceCodec`, `CustomSet`, `PromptThings`,
  `CommonFields`. All pure functions, no framework deps.
- `src/services/apiClient.ts` — `fetchApi`, `apiClient`, CSRF,
  error normalization. The error channel is exposed via
  `onGlobalError(handler)` so the host can wire its own UI.
- `src/components.ts` — the `FRPLM_COMPONENT_TAGS` map.
- `src/HostSdkBindings.ts` — the `FrplmHostBindings` /
  `FrplmExtensionBindings` interfaces.

`vue` is a **peer dependency** — used only by `reactive()` inside
`ABSEntity` to keep entity data reactive. The SDK does not import
any Vue component, plugin, or template API.

### `ui/` — `@frplm/ui`

Reusable Vue 3 primitive editor components. Each one is a single
SFC. No business logic.

- `src/components/` — `AutoCompleteBox`, `BooleanTickBox`,
  `BooleanToggle`, `LongTextBox`, `NumberInput`, `NumberSlider`,
  `ShortTextBox`, `SingleAutoComplete`, `SingleEnumInput`.
- `src/index.ts` — public entry. Re-exports every component plus
  the `TokenCounter` type that `LongTextBox` accepts as a prop.

The build outputs `dist/index.js` (ESM) and `dist/style.css`
(scoped styles of every component bundled together). The host
loads `style.css` once at startup; extensions inherit the look
automatically.

> `LongTextBox` accepts a `countTokens: (text: string) => Promise<number>`
> prop instead of calling a tokenizer directly. The host injects the
> real backend-backed function; the UI never reaches into the SDK or
> the network.

### `app/` — `@frplm/frontend`

The host Vue 3 SPA. **Private — extensions must not import from it.**

- `src/main.js` — entry point. Wires the router, theme, global
  error queue, primitive-editor custom elements, and
  `window.FrplmHost` / `window.FrplmExtension` bindings.
- `src/App.vue` — root layout (header, nav, error region, router view).
- `src/app/router.ts` — the application router.
- `src/components/` — every app-level component, grouped by feature
  area (`chat/`, `space/`, `lorebooks/`, ...). **None of these are
  part of the public UI library.**
- `src/core/theme.ts` — the app theme controller.
- `src/core/GlobalError.ts` — the app error queue. Wires the SDK's
  `onGlobalError` channel into Vue reactive state.
- `src/services/tokenizer.ts` — the app's tokenizer state (which
  LLM connection to use for token counting). This is **app-level**
  state, not part of the SDK.
- `src/extensions/HostBindings.ts` — populates `window.FrplmHost`
  and `window.FrplmExtension` so extensions can interact with the
  app.
- `src/extensions/registerPrimitiveEditors.ts` — registers the
  `@frplm/ui` components as custom elements using
  `FRPLM_COMPONENT_TAGS` from the SDK.
- `src/assets/` — CSS and SVG assets for the app shell.

The `app/vite.config.js` aliases `@frplm/host-sdk` and `@frplm/ui`
to the local `dist/` outputs of the sibling workspaces (see
`workspace-aliases.mjs`). Vite resolves `@frplm/ui/style.css` to
`ui/dist/style.css`.

## Build and test

Each package is independent. From a package directory:

```sh
npm run build       # build the package
npm test            # run the package's tests
npm run typecheck   # tsc --noEmit
```

A full pass from the repo root:

```sh
( cd sdk && npm run build && npm test )
( cd ui  && npm run build && npm test )
( cd app && npm run build )
```

The SDK and UI **must be built before the app** when running
`npm run build` on the app, because the app's Vite alias points at
the compiled `dist/index.js`. For `npm run dev` the same rule
applies — Vite will warn and fail if the dist files are missing.

To skip the build step during SDK/UI iteration, change
`app/workspace-aliases.mjs` to point at the source `.ts` files
directly. This is slower per dev request but removes the rebuild
friction.

## Common tasks for maintainers

### I want to add a new domain entity

You almost certainly want to add it to the SDK, not the app.

1. Create `sdk/src/domain/MyEntity.ts`. Extend `ABSEntity` and
   define the `Key` and `Data` types.
2. Add it to `sdk/src/index.ts`'s exports.
3. Add it to `sdk/src/components.ts`'s `FRPLM_COMPONENT_TAGS` if
   extension authors need to reference it by component name.
4. If the entity has a primitive editor, the editor itself lives
   in `ui/`, **not** in `sdk/`. The SDK only owns the type.
5. Run `sdk` tests and `app` build to make sure nothing regressed.

### I want to add a new primitive editor

Add it to the UI library, not the app.

1. Create `ui/src/components/MyField.vue`. Keep it framework-only —
   no API calls, no theme logic. Accept a `modelValue` and emit
   `edit`. If the field needs external information, accept it as a
   prop or via an injected callback.
2. Export it from `ui/src/index.ts`.
3. Register its tag in `sdk/src/components.ts`'s
   `FRPLM_COMPONENT_TAGS` map. The SDK is the source of truth for
   tag names because extensions need them.
4. Add a smoke test in `ui/tests/`.

### I want to change how a host binding is exposed

The contract lives in `sdk/src/HostSdkBindings.ts`. The
implementation lives in `app/src/extensions/HostBindings.ts`.

1. Update the `FrplmHostBindings` or `FrplmExtensionBindings`
   interface in the SDK. This is a **breaking change** for
   extensions — bump the SDK major version and coordinate with
   extension authors.
2. Update `app/src/extensions/HostBindings.ts` so the runtime
   value satisfies the new interface.

### I want to add a new app page

Pure app work. Lives in `app/src/components/<area>/`. Register the
route in `app/src/app/router.ts`. No SDK or UI changes needed.

## Things to be careful about

- **Do not import Vue from the SDK.** The only Vue import allowed in
  `sdk/src/` is `reactive` inside `ABSEntity`. Adding Vue
  components, `defineComponent`, or any template API to the SDK
  breaks the framework-agnostic contract.
- **Do not import `@frplm/frontend` from anything.** The app
  package is private. Extensions import from `@frplm/host-sdk`
  and `@frplm/ui` only.
- **Do not reach into the SDK's `dist/` from a test.** Tests in
  `sdk/tests/` import from the source via relative paths. Tests
  in `ui/tests/` and `app/tests/` import from the built `dist/`
  because that's what the package exports.
- **The host theme is in the app, not the SDK.** CSS custom
  properties are defined in `app/src/assets/main.css` and consumed
  by both the app and the UI. Adding new theme tokens? Update
  `app/src/assets/main.css` and the corresponding usage in
  `ui/src/components/`.
- **Vue is a peer dependency of both `sdk/` and `ui/`.** When
  bumping Vue, check that both packages still type-check.

## Publishing

`@frplm/host-sdk` and `@frplm/ui` are published to npm. They are
versioned independently:

- The SDK is a **stable contract** consumed by every extension.
  Breaking changes are a major version bump and must be coordinated
  with extension authors.
- The UI is a **styled component library**. Its version matches
  the SDK major version when a change is breaking for extension
  consumers; otherwise it is bumped independently.

`@frplm/frontend` is **private** and not published. Its `private`
field in `package.json` is set to `true`.

## Versioning policy

| Change                                          | Bump                                |
|-------------------------------------------------|-------------------------------------|
| Add a new entity, type, or utility in the SDK   | minor                               |
| Rename or remove a public symbol in the SDK     | **major** (breaking for extensions) |
| Add a new primitive editor in the UI             | minor                               |
| Remove or rename a primitive editor              | **major** (breaking for extensions) |
| Change the host binding contract                | **major** (breaking for extensions) |
| Fix a bug, refactor, or add an app page         | patch                               |
