<script setup lang="ts">
import {     LongTextBox,
    ShortTextBox } from "@frplm/ui";

import {     Region,
    Lorebook } from "@frplm/host-sdk";

import {computed, onMounted, ref, shallowRef, watch} from "vue";

import type {RegionData} from "@frplm/host-sdk";

import LorebookEditor from "@components/lorebooks/LorebookEditor.vue";
import FieldEditorWrapper from "@components/utils/FieldEditorWrapper.vue";
import Expandable from "@components/utils/panels/Expandable.vue";

const props = defineProps<{
    region: Region;
}>();

const lorebook = shallowRef<Lorebook | null>(null);
const loadingLorebook = ref(false);
const loadError = ref<string | null>(null);
const saveError = ref<string | null>(null);
const savingFields = ref<ReadonlySet<keyof RegionData>>(new Set());
let loadRequestId = 0;

const regionName = computed(() => {
    const name = String(props.region.get("name") ?? "").trim();
    return name || "Unnamed region";
});

const isSaving = computed(() => savingFields.value.size > 0);

function markFieldSaving(field: keyof RegionData, saving: boolean): void {
    const next = new Set(savingFields.value);
    if (saving) next.add(field);
    else next.delete(field);
    savingFields.value = next;
}

async function updateField<K extends keyof RegionData>(field: K, value: RegionData[K]): Promise<void> {
    if (savingFields.value.has(field) || props.region.get(field) === value) return;

    saveError.value = null;
    markFieldSaving(field, true);

    try {
        const updated = await props.region.update(field, value);
        if (updated === false) throw new Error("Region update returned false");
    } catch (error) {
        console.error(`Could not update region field ${String(field)}`, error);
        saveError.value = "The region information could not be saved.";
    } finally {
        markFieldSaving(field, false);
    }
}

async function loadLorebook(): Promise<void> {
    const requestId = ++loadRequestId;
    lorebook.value = null;
    loadingLorebook.value = true;
    loadError.value = null;

    try {
        const loaded = await props.region.getLorebook();
        if (requestId === loadRequestId) lorebook.value = loaded;
    } catch (error) {
        if (requestId !== loadRequestId) return;
        console.error("Could not load region lorebook", error);
        loadError.value = "The region lorebook could not be loaded.";
    } finally {
        if (requestId === loadRequestId) loadingLorebook.value = false;
    }
}

onMounted(() => void loadLorebook());

watch(
    [
        () => props.region.get("world_id"),
        () => props.region.get("id"),
    ],
    () => {
        savingFields.value = new Set();
        saveError.value = null;
        void loadLorebook();
    },
);
</script>

<template>
    <article class="region-editor" :aria-busy="isSaving || loadingLorebook">
        <header class="region-editor__header">
            <div>
                <span class="region-editor__eyebrow">Region information</span>
                <h2 class="region-editor__title">{{ regionName }}</h2>
                <p class="region-editor__description">
                    Edit descriptive data for this region. Hierarchy, geometry, collapse state, backgrounds, contained entities, and deletion remain graph responsibilities.
                </p>
            </div>

        </header>

        <div v-if="saveError" class="region-editor__error" role="alert">
            <span>{{ saveError }}</span>
            <button type="button" @click="saveError = null">Dismiss</button>
        </div>

        <section class="region-editor__section">
            <FieldEditorWrapper field-name="Name" info="The region's display name." :vertical="true">
                <ShortTextBox
                    :model-value="region.get('name')"
                    aria-label="Region name"
                    @edit="value => updateField('name', value)"
                />
            </FieldEditorWrapper>

            <FieldEditorWrapper field-name="Description" info="Narrative and semantic context associated with the region." :vertical="true">
                <LongTextBox
                    :model-value="region.get('description')"
                    aria-label="Region description"
                    tokenize
                    :tokenization-started="true"
                    @edit="value => updateField('description', value)"
                />
            </FieldEditorWrapper>
        </section>

        <section class="region-editor__section region-editor__section--accent">
            <Expandable title="Region lorebook" info="Lore scoped to this region and its narrative context." :initially-open="false">
                <LorebookEditor v-if="lorebook" :model-value="lorebook" />

                <div v-else-if="loadingLorebook" class="region-editor__state" role="status">
                    Loading region lorebook…
                </div>

                <div v-else-if="loadError" class="region-editor__state region-editor__state--error" role="alert">
                    <span>{{ loadError }}</span>
                    <button type="button" @click="loadLorebook">Retry</button>
                </div>

                <div v-else class="region-editor__state">
                    This region does not currently expose a lorebook.
                </div>
            </Expandable>
        </section>
    </article>
</template>

<style scoped>
.region-editor {
    display: grid;
    gap: var(--space-5);
    min-width: 0;
    color: rgb(var(--c-fg));
}

.region-editor__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-4);
}

.region-editor__eyebrow {
    color: rgb(var(--c-muted));
    font-size: 0.72rem;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.region-editor__title {
    margin: var(--space-1) 0 0;
    color: rgb(var(--c-fg-strong));
    font-size: 1.35rem;
    overflow-wrap: anywhere;
}

.region-editor__description {
    margin: var(--space-2) 0 0;
    color: rgb(var(--c-muted));
    font-size: 0.9rem;
    line-height: 1.5;
}

.region-editor__status {
    flex: 0 0 auto;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-round);
    background: rgb(var(--c-info-soft));
    color: rgb(var(--c-info-strong));
    font-size: 0.75rem;
    font-weight: 800;
}

.region-editor__section {
    display: grid;
    gap: var(--space-4);
    min-width: 0;
    padding: var(--space-4);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-lg);
    background: rgb(var(--c-surface-raised));
}

.region-editor__section--accent {
    border-color: rgb(var(--c-accent-2) / 0.6);
}

.region-editor__error,
.region-editor__state--error {
    border-color: rgb(var(--c-danger) / 0.5);
    background: rgb(var(--c-danger-soft));
    color: rgb(var(--c-danger-strong));
}

.region-editor__error,
.region-editor__state {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-3);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-md);
}

.region-editor button {
    border: 1px solid currentColor;
    border-radius: var(--radius-sm);
    background: transparent;
    color: inherit;
    cursor: pointer;
}
</style>
