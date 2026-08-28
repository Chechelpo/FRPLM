// ui/src/shims-vue.d.ts
/**
 * Ambient module declarations that let `tsc` resolve `.vue` files
 * when type-checking the UI package. Type information inside SFCs
 * is provided by vue-tsc at build time; this shim is just enough to
 * keep the bundler and tsc happy.
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
