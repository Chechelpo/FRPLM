<script setup lang="ts">
import {     Location,
    LocationEdge } from "@frplm/host-sdk";

import {computed, onMounted, ref, shallowRef, watch} from "vue";


import EdgeEditor from "@components/space/EdgeEditor.vue";
import {publishEdgeTraversability} from "@components/space/world_graph/utils/edgeSync";

const props = withDefaults(defineProps<{
    first: Location;
    second: Location;
    refreshToken?: number;
}>(), {
    refreshToken: 0,
});

const forwardEdge = shallowRef<LocationEdge | null>(null);
const reverseEdge = shallowRef<LocationEdge | null>(null);
const loading = ref(false);
const loadError = ref<string | null>(null);
let loadRequestId = 0;

const firstName = computed(() => locationName(props.first));
const secondName = computed(() => locationName(props.second));
const pairLabel = computed(() => `${firstName.value} ↔ ${secondName.value}`);
const existingDirectionCount = computed(() => Number(forwardEdge.value !== null) + Number(reverseEdge.value !== null));

function locationName(location: Location): string {
    const name = String(location.get("name") ?? "").trim();
    return name || "Unnamed location";
}

function assertValidPair(): void {
    if (props.first.equals(props.second)) throw new Error("A connection pair requires two distinct locations");
    if (props.first.get("worldID") !== props.second.get("worldID")) throw new Error("Connection endpoints must belong to the same world");
}

function findDirection(edges: readonly LocationEdge[], destination: Location): LocationEdge | null {
    const destinationId = destination.get("id");
    return edges.find(edge => edge.get("to_id") === destinationId) ?? null;
}

async function load(): Promise<void> {
    const requestId = ++loadRequestId;
    loading.value = true;
    loadError.value = null;

    try {
        assertValidPair();
        const [firstOutgoing, secondOutgoing] = await Promise.all([
            props.first.getOutEdges(),
            props.second.getOutEdges(),
        ]);

        if (requestId !== loadRequestId) return;
        forwardEdge.value = findDirection(firstOutgoing, props.second);
        reverseEdge.value = findDirection(secondOutgoing, props.first);
    } catch (error) {
        if (requestId !== loadRequestId) return;
        console.error("Could not load connection information", error);
        forwardEdge.value = null;
        reverseEdge.value = null;
        loadError.value = "The connection information could not be loaded.";
    } finally {
        if (requestId === loadRequestId) loading.value = false;
    }
}

onMounted(() => void load());

watch(
    [
        () => props.first.get("worldID"),
        () => props.first.get("id"),
        () => props.second.get("worldID"),
        () => props.second.get("id"),
        () => props.refreshToken,
    ],
    () => void load(),
);

watch(
    [
        () => forwardEdge.value?.get("is_traversable"),
        () => reverseEdge.value?.get("is_traversable"),
    ],
    () => {
        if (forwardEdge.value !== null) publishEdgeTraversability(forwardEdge.value);
        if (reverseEdge.value !== null) publishEdgeTraversability(reverseEdge.value);
    },
    {flush: "sync"},
);
</script>

<template>
    <section class="connection-editor" :aria-busy="loading">
        <header class="connection-editor__header">
            <div>
                <span class="connection-editor__eyebrow">Connection information</span>
                <h2 class="connection-editor__title">{{ pairLabel }}</h2>
                <p class="connection-editor__description">
                    Edit metadata for the directed edges that already exist between this location pair.
                </p>
            </div>

            <button type="button" class="connection-editor__reload" :disabled="loading" @click="load">
                {{ loading ? "Loading…" : "Reload" }}
            </button>
        </header>

        <div class="connection-editor__summary" aria-live="polite">
            <strong>{{ existingDirectionCount }}</strong>
            <span>{{ existingDirectionCount === 1 ? "existing direction" : "existing directions" }}</span>
        </div>

        <div v-if="loadError" class="connection-editor__state connection-editor__state--error" role="alert">
            <strong>Connection unavailable</strong>
            <span>{{ loadError }}</span>
            <button type="button" @click="load">Retry</button>
        </div>

        <div v-else-if="loading" class="connection-editor__state" role="status">
            <strong>Loading connection</strong>
            <span>Retrieving both possible directed edges.</span>
        </div>

        <div v-else class="connection-editor__directions">
            <EdgeEditor
                v-if="forwardEdge"
                :key="forwardEdge.hashKey()"
                :source="first"
                :destination="second"
                :edge="forwardEdge"
            />

            <div v-else class="connection-editor__state connection-editor__state--missing">
                <strong>{{ firstName }} → {{ secondName }} does not exist</strong>
                <span>Create or delete directed connections from the graph context menu.</span>
            </div>

            <EdgeEditor
                v-if="reverseEdge"
                :key="reverseEdge.hashKey()"
                :source="second"
                :destination="first"
                :edge="reverseEdge"
            />

            <div v-else class="connection-editor__state connection-editor__state--missing">
                <strong>{{ secondName }} → {{ firstName }} does not exist</strong>
                <span>Create or delete directed connections from the graph context menu.</span>
            </div>
        </div>
    </section>
</template>

<style scoped>
.connection-editor {
    display: grid;
    gap: var(--space-4);
    min-width: 0;
    color: rgb(var(--c-fg));
}

.connection-editor__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-4);
}

.connection-editor__eyebrow {
    color: rgb(var(--c-muted));
    font-size: 0.72rem;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.connection-editor__title {
    margin: var(--space-1) 0 0;
    color: rgb(var(--c-fg-strong));
    font-size: 1.25rem;
    overflow-wrap: anywhere;
}

.connection-editor__description {
    margin: var(--space-2) 0 0;
    color: rgb(var(--c-muted));
    font-size: 0.9rem;
    line-height: 1.5;
}

.connection-editor__reload,
.connection-editor__state button {
    flex: 0 0 auto;
    padding: var(--space-2) var(--space-3);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-sm);
    background: rgb(var(--c-surface));
    color: rgb(var(--c-fg));
    cursor: pointer;
}

.connection-editor__reload:disabled {
    cursor: wait;
    opacity: 0.65;
}

.connection-editor__summary {
    display: flex;
    align-items: baseline;
    gap: var(--space-2);
    padding: var(--space-3);
    border-radius: var(--radius-md);
    background: rgb(var(--c-surface-2));
    color: rgb(var(--c-muted));
}

.connection-editor__summary strong {
    color: rgb(var(--c-fg-strong));
    font-size: 1.25rem;
}

.connection-editor__directions {
    display: grid;
    gap: var(--space-4);
    min-width: 0;
}

.connection-editor__state {
    display: grid;
    gap: var(--space-2);
    padding: var(--space-4);
    border: 1px dashed rgb(var(--c-border));
    border-radius: var(--radius-lg);
    background: rgb(var(--c-surface));
    color: rgb(var(--c-muted));
}

.connection-editor__state strong {
    color: rgb(var(--c-fg-strong));
}

.connection-editor__state--missing {
    background: rgb(var(--c-surface-2) / 0.55);
}

.connection-editor__state--error {
    border-style: solid;
    border-color: rgb(var(--c-danger) / 0.5);
    background: rgb(var(--c-danger-soft));
    color: rgb(var(--c-danger-strong));
}
</style>
