// workspace-aliases.mjs
/**
 * Resolve the on-disk paths of the SDK and UI workspaces so Vite and
 * TypeScript can alias them. The two packages are checked out as
 * siblings of this `app/` directory:
 *
 *   <repo-root>/
 *     app/
 *     sdk/
 *     ui/
 *
 * The aliases are computed relative to this file's location so the
 * configuration works no matter where the repo is checked out.
 */
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, "..");

export const hostSdkPath = resolve(repoRoot, "sdk", "dist", "index.js");
export const uiPath = resolve(repoRoot, "ui", "dist", "index.js");
export const hostSdkSource = resolve(repoRoot, "sdk", "src", "index.ts");
export const uiSource = resolve(repoRoot, "ui", "src", "index.ts");
export const hostSdkDist = resolve(repoRoot, "sdk", "dist");
export const uiDist = resolve(repoRoot, "ui", "dist");
