# `@frplm/ui`

Reusable Vue 3 primitive editor components for FRPLM. The package
exists so that:

1. The host application and every extension panel share the **same
   look and feel** for value editing.
2. Extension authors do not have to re-implement text boxes, number
   inputs, autocompletes, toggles, etc. — they pick a primitive
   editor from the registry and let it inherit the host's theme.

> **Ownership.** This package is maintained by AI agents. It is
> intentionally thin: every component is a single Vue SFC, depends
> only on `vue`, and reads theme variables from CSS custom
> properties exposed by the host's stylesheet. Do not add app-level
> concerns (router, global state, business logic) to this package.

## What lives here

- **Primitive editors** (`src/components/`):
  - `AutoCompleteBox`
  - `BooleanTickBox`
  - `BooleanToggle`
  - `LongTextBox`
  - `NumberInput`
  - `NumberSlider`
  - `ShortTextBox`
  - `SingleAutoComplete`
  - `SingleEnumInput`
- **Compiled stylesheet** (`dist/style.css` after build): every
  scoped style of the components above.

## How to consume

```ts
import {
    LongTextBox,
    NumberInput,
    ShortTextBox,
    BooleanToggle,
    type TokenCounter,
} from "@frplm/ui";
import "@frplm/ui/style.css";
```

The matching `style.css` only has to be loaded **once** per
application — the host loads it in `main.ts`, and extensions get it
implicitly through the host.

`LongTextBox` opts into token counting when the host passes a
`countTokens` function:

```vue
<LongTextBox
    v-model="content"
    :count-tokens="countTokens"
    tokenize
    tokenization-started
    @edit="value => entity.update('content', value)"
/>
```

The function is intentionally injected by the host so this package
never reaches into the SDK or the backend directly.

## Dependency model

- `vue` is a **peer dependency**.
- No runtime dependencies on the SDK or the host app.

## Build and test

```sh
npm run build       # vite library build -> dist/index.js + dist/style.css
npm test            # build then run smoke tests
npm run typecheck   # tsc --noEmit
```

`dist/index.js` is a single ES module that bundles every component.
`vue` is treated as an external so consumers always resolve the
same instance via Vite's `optimizeDeps`.

## Used by

- The host application (`app/`).
- Every published FRPLM extension panel.
