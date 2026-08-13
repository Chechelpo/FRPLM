import {computed, type ComputedRef, onBeforeUnmount, type Ref, shallowRef, watch,} from "vue";
import type {Region, World} from "@/domain/World";
import type {BackgroundTarget, WorldGraphData} from "../types";
import {regionEntityKey} from "../utils/geometry";
import {EntityAssetType} from "@/core/ABSEntity";

export function backgroundTargetKey(target: BackgroundTarget): string {
    return target.kind === "world"
        ? `world:${target.world.get("id")}`
        : regionEntityKey(target.region);
}

export function isSupportedImageBlob(blob: Blob): boolean {
    return blob.size > 0 && blob.type.toLowerCase().startsWith("image/");
}

export async function decodeImageBlob(blob: Blob): Promise<void> {
    if (blob.size <= 0) {
        throw new Error("The selected image is empty");
    }

    try {
        if (typeof createImageBitmap === "function") {
            const bitmap = await createImageBitmap(blob);

            try {
                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    throw new Error("The image has invalid dimensions");
                }
            } finally {
                bitmap.close();
            }

            return;
        }

        if (typeof Image === "undefined" || typeof URL === "undefined") {
            throw new Error("No image decoder is available in this environment");
        }

        const url = URL.createObjectURL(blob);

        try {
            await new Promise<void>((resolve, reject) => {
                const image = new Image();

                image.onload = () => {
                    if (
                        image.naturalWidth > 0 &&
                        image.naturalHeight > 0
                    ) {
                        resolve();
                    } else {
                        reject(
                            new Error("The image has invalid dimensions")
                        );
                    }
                };

                image.onerror = () => {
                    reject(
                        new Error("The selected image could not be decoded")
                    );
                };

                image.src = url;
            });
        } finally {
            URL.revokeObjectURL(url);
        }
    } catch (error) {
        const declaredType =
            blob.type.trim() || "<missing>";

        throw new Error(
            `Image decoding failed; declared MIME=${declaredType}, size=${blob.size}`,
            { cause: error }
        );
    }
}
export function useWorldGraphBackgrounds(
    world: ComputedRef<World>,
    graph: Ref<WorldGraphData>,
    reportError: (message: string) => void,
) {
    const worldUrl = shallowRef<string | null>(null);
    const regionUrls = shallowRef<ReadonlyMap<string, string>>(new Map());
    const pendingKeys = shallowRef<ReadonlySet<string>>(new Set());
    const failedKeys = shallowRef<ReadonlySet<string>>(new Set());
    const generations = new Map<string, number>();

    function revoke(url: string | null): void {
        if (url !== null && typeof URL !== "undefined") URL.revokeObjectURL(url);
    }

    function nextGeneration(key: string): number {
        const next = (generations.get(key) ?? 0) + 1;
        generations.set(key, next);
        return next;
    }

    function isLatest(key: string, generation: number): boolean {
        return generations.get(key) === generation;
    }

    function replaceWorldUrl(url: string | null): void {
        const previous = worldUrl.value;
        worldUrl.value = url;
        if (previous !== url) revoke(previous);
    }

    function replaceRegionUrl(key: string, url: string | null): void {
        const next = new Map(regionUrls.value);
        const previous = next.get(key) ?? null;
        if (url === null) next.delete(key);
        else next.set(key, url);
        regionUrls.value = next;
        if (previous !== url) revoke(previous);
    }

    async function fetchTargetBlob(target: BackgroundTarget): Promise<Blob | null> {
        return fetchGraphBackgroundBlob(target);
    }

    async function uploadTarget(target: BackgroundTarget, file: File | Blob): Promise<void> {
        if (target.kind === "world") await target.world.postAsset(EntityAssetType.BACKGROUND, file, true);
        else await target.region.saveBackground(file, true);
    }

    async function deleteTarget(target: BackgroundTarget): Promise<void> {
        if (target.kind === "world") await target.world.deleteAsset(EntityAssetType.BACKGROUND);
        else await target.region.deleteBackground();
    }

    async function refreshTarget(target: BackgroundTarget): Promise<boolean> {
        const key = backgroundTargetKey(target);
        const generation = nextGeneration(key);
        try {
            const blob = await fetchTargetBlob(target);
            if (!isLatest(key, generation)) return false;
            if (blob === null) {
                if (target.kind === "world") replaceWorldUrl(null);
                else replaceRegionUrl(key, null);
                return true;
            }
            await decodeImageBlob(blob);
            if (!isLatest(key, generation)) return false;
            const url = URL.createObjectURL(blob);
            if (target.kind === "world") replaceWorldUrl(url);
            else replaceRegionUrl(key, url);
            return true;
        } catch (error) {
            console.debug("Unable to load a graph background", error);
            return false;
        }
    }

    async function setBackground(target: BackgroundTarget, file: File): Promise<boolean> {
        const key = backgroundTargetKey(target);
        const expectedWorldId = world.value.get("id");
        pendingKeys.value = new Set([...pendingKeys.value, key]);
        failedKeys.value = new Set([...failedKeys.value].filter((item) => item !== key));

        try {
            await decodeImageBlob(file);
            await uploadTarget(target, file);
            if (world.value.get("id") !== expectedWorldId) return false;
            const refreshed = await refreshTarget(target);
            if (!refreshed) throw new Error("Stored background could not be refreshed");
            return true;
        } catch (error) {
            console.error("Unable to replace a graph background", error);
            if (world.value.get("id") === expectedWorldId) {
                failedKeys.value = new Set([...failedKeys.value, key]);
                reportError("The background could not be replaced. The previous canvas remains unchanged.");
            }
            return false;
        } finally {
            if (world.value.get("id") === expectedWorldId) {
                pendingKeys.value = new Set([...pendingKeys.value].filter((item) => item !== key));
            }
        }
    }

    async function deleteBackground(target: BackgroundTarget): Promise<boolean> {
        const key = backgroundTargetKey(target);
        const expectedWorldId = world.value.get("id");
        pendingKeys.value = new Set([...pendingKeys.value, key]);
        failedKeys.value = new Set([...failedKeys.value].filter((item) => item !== key));

        try {
            await deleteTarget(target);
            if (world.value.get("id") !== expectedWorldId) return false;
            nextGeneration(key);
            if (target.kind === "world") replaceWorldUrl(null);
            else replaceRegionUrl(key, null);
            return true;
        } catch (error) {
            console.error("Unable to delete a graph background", error);
            if (world.value.get("id") === expectedWorldId) {
                failedKeys.value = new Set([...failedKeys.value, key]);
                reportError("The background could not be deleted.");
            }
            return false;
        } finally {
            if (world.value.get("id") === expectedWorldId) {
                pendingKeys.value = new Set([...pendingKeys.value].filter((item) => item !== key));
            }
        }
    }

    function regionUrl(region: Region): string | null {
        return regionUrls.value.get(regionEntityKey(region)) ?? null;
    }

    function hasRegionBackground(region: Region): boolean {
        return regionUrl(region) !== null;
    }

    function isBackgroundPending(target: BackgroundTarget): boolean {
        return pendingKeys.value.has(backgroundTargetKey(target));
    }

    function isBackgroundFailed(target: BackgroundTarget): boolean {
        return failedKeys.value.has(backgroundTargetKey(target));
    }

    const hasWorldBackground = computed(() => worldUrl.value !== null);

    watch(
        () => world.value.get("id"),
        async () => {
            for (const key of generations.keys()) nextGeneration(key);
            replaceWorldUrl(null);
            for (const url of regionUrls.value.values()) revoke(url);
            regionUrls.value = new Map();
            pendingKeys.value = new Set();
            failedKeys.value = new Set();
            await refreshTarget({kind: "world", world: world.value});
        },
        {immediate: true},
    );

    watch(
        () => graph.value.regions.map(regionEntityKey).join("|"),
        () => {
            const currentKeys = new Set(graph.value.regions.map(regionEntityKey));
            const next = new Map(regionUrls.value);
            for (const [key, url] of next) {
                if (!currentKeys.has(key)) {
                    next.delete(key);
                    revoke(url);
                    nextGeneration(key);
                }
            }
            regionUrls.value = next;
            for (const region of graph.value.regions) {
                const key = regionEntityKey(region);
                if (!regionUrls.value.has(key)) void refreshTarget({kind: "region", region});
            }
        },
        {immediate: true},
    );

    onBeforeUnmount(() => {
        replaceWorldUrl(null);
        for (const url of regionUrls.value.values()) revoke(url);
        regionUrls.value = new Map();
    });

    return {
        worldUrl,
        regionUrls,
        pendingKeys,
        failedKeys,
        hasWorldBackground,
        regionUrl,
        hasRegionBackground,
        isBackgroundPending,
        isBackgroundFailed,
        refreshTarget,
        setBackground,
        deleteBackground,
    };
}

/** Loads one background without exposing replacement or deletion operations. */
export async function fetchGraphBackgroundBlob(
    target: BackgroundTarget,
): Promise<Blob | null> {
    return target.kind === "world"
        ? target.world.getAsset(EntityAssetType.BACKGROUND)
        : target.region.fetchBackground();
}

/**
 * Read-only background lifecycle for compact graph projections.
 *
 * The returned object URL is replaced atomically and revoked after target
 * changes or component teardown. Stale asynchronous completions are ignored.
 */
export function useWorldGraphBackgroundAsset(
    target: ComputedRef<BackgroundTarget | null>,
) {
    const url = shallowRef<string | null>(null);
    const isLoading = shallowRef(false);
    const failed = shallowRef(false);
    let generation = 0;

    function revoke(value: string | null): void {
        if (value !== null && typeof URL !== "undefined") {
            URL.revokeObjectURL(value);
        }
    }

    function replaceUrl(value: string | null): void {
        const previous = url.value;
        url.value = value;
        if (previous !== value) revoke(previous);
    }

    async function load(): Promise<boolean> {
        const activeGeneration = ++generation;
        const activeTarget = target.value;
        failed.value = false;

        if (activeTarget === null) {
            replaceUrl(null);
            isLoading.value = false;
            return true;
        }

        isLoading.value = true;
        try {
            const blob = await fetchGraphBackgroundBlob(activeTarget);
            if (activeGeneration !== generation) return false;
            if (blob === null) {
                replaceUrl(null);
                return true;
            }

            await decodeImageBlob(blob);
            if (activeGeneration !== generation) return false;
            if (typeof URL === "undefined") {
                throw new Error("Object URLs are unavailable in this environment");
            }

            replaceUrl(URL.createObjectURL(blob));
            return true;
        } catch (error) {
            if (activeGeneration !== generation) return false;
            console.error("Unable to load a graph background", error);
            failed.value = true;
            replaceUrl(null);
            return false;
        } finally {
            if (activeGeneration === generation) {
                isLoading.value = false;
            }
        }
    }

    watch(
        () => target.value === null
            ? null
            : backgroundTargetKey(target.value),
        () => {
            void load();
        },
        {immediate: true},
    );

    onBeforeUnmount(() => {
        generation += 1;
        replaceUrl(null);
    });

    return {
        url,
        isLoading,
        failed,
        reload: load,
    };
}

