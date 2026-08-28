// app/src/shims-vue.d.ts
/**
 * Ambient module declaration that lets `tsc` resolve `.vue` files
 * when type-checking the app. The real type information is provided
 * by `vue-tsc` at build time; this shim only keeps `tsc` happy when
 * it walks the dependency graph during plain `tsc --noEmit`.
 */
declare module "*.vue" {
    import type { DefineComponent } from "vue";
    const component: DefineComponent<
        Record<string, unknown>,
        Record<string, unknown>,
        unknown
    >;
    export default component;
}
