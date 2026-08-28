# `@frplm/frontend`

The FRPLM host application. A Vue 3 + Vite SPA that consumes the
SDK and UI packages and renders the user-facing interface.

> **Ownership.** This package is maintained by AI agents. The
> project owner does not write UI code. Keep the package's
> responsibilities focused: anything that is reusable outside the
> host (domain types, generic components, helpers) belongs in
> `@frplm/host-sdk` or `@frplm/ui`, not here.

## What lives here

- `src/main.js` — entry point. Wires the router, theme, global
  error queue, primitive-editor custom elements, and `window.FrplmHost`
  / `window.FrplmExtension` bindings.
- `src/App.vue` — root layout (header, nav, error region, router
  view).
- `src/app/router.ts` — the application router.
- `src/components/` — every component that is *not* a primitive
  editor. Subdirectories group components by feature area
  (`chat/`, `space/`, `lorebooks/`, ...).
- `src/core/theme.ts` — the app-level theme controller.
- `src/core/GlobalError.ts` — the app-level error queue. Wires
  `@frplm/host-sdk`'s `onGlobalError` channel into the Vue
  reactive state consumed by `components/errors/GlobalError.vue`.
- `src/services/tokenizer.ts` — the app-level tokenizer state
  (which LLM connection is currently used for token counting).
- `src/extensions/HostBindings.ts` — populates
  `window.FrplmHost` and `window.FrplmExtension` so that
  separately-published extension panels can interact with the
  application.
- `src/extensions/registerPrimitiveEditors.ts` — registers the
  `@frplm/ui` components as custom elements using
  `FRPLM_COMPONENT_TAGS` from the SDK.
- `src/assets/` — CSS and SVG assets (logo, theme, layout).

## How to develop

```sh
# One-time: build the SDK and UI
( cd ../sdk && npm run build )
( cd ../ui  && npm run build )

# Then from this directory
npm install
npm run dev
```

The dev server proxies `/api` to `http://127.0.0.1:8080`, where the
FRPLM backend normally runs.

## Build

```sh
npm run build       # vite production build -> dist/
```

## Tests

The app is currently a thin composition layer; its tests are
integration-level (the SPA builds and starts without errors). When
the project grows, place feature-specific tests under
`tests/<feature>/`.

```sh
npm test            # builds the app; smoke check
```

## Aliases

The app uses path aliases that resolve to its own source tree
plus the two sibling packages:

- `@`             → `src/`
- `@app/*`        → `src/app/*`
- `@components/*` → `src/components/*`
- `@extensions/*` → `src/extensions/*`
- `@core/*`       → `src/core/*`
- `@services/*`   → `src/services/*`
- `@assets/*`     → `src/assets/*`
- `@frplm/host-sdk` → `../sdk/src/index.ts` (TypeScript),
                       `../sdk/dist/index.js` (Vite)
- `@frplm/ui`       → `../ui/src/index.ts` (TypeScript),
                       `../ui/dist/index.js` (Vite)
- `@frplm/ui/style.css` → `../ui/dist/style.css` (Vite)
