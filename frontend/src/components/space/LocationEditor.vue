<script setup lang="ts">
import {computed, onMounted, ref, shallowRef, watch} from "vue";

import type {LocationData} from "@/domain/World";
import {Location} from "@/domain/World";
import {Lorebook} from "@/domain/Lorebook";

import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LongTextBox from "@/components/primitive-editors/LongTextBox.vue";
import ShortTextBox from "@/components/primitive-editors/ShortTextBox.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import LocationStartingCharactersEditor from "@/components/char/LocationStartingCharactersEditor.vue";

const props = defineProps<{
    location: Location;
}>();

const lorebook = shallowRef<Lorebook | null>(null);
const loadingLorebook = ref(false);
const loadError = ref<string | null>(null);
const saveError = ref<string | null>(null);
const savingFields = ref<ReadonlySet<keyof LocationData>>(new Set());
let loadRequestId = 0;

const locationName = computed(() => {
    const name = String(props.location.get("name") ?? "").trim();
    return name || "Unnamed location";
});

const isSaving = computed(() => savingFields.value.size > 0);

function markFieldSaving(field: keyof LocationData, saving: boolean): void {
    const next = new Set(savingFields.value);
    if (saving) next.add(field);
    else next.delete(field);
    savingFields.value = next;
}

async function updateField<K extends keyof LocationData>(field: K, value: LocationData[K]): Promise<void> {
    if (savingFields.value.has(field) || props.location.get(field) === value) return;

    saveError.value = null;
    markFieldSaving(field, true);

    try {
        const updated = await props.location.update(field, value);
        if (updated === false) throw new Error("Location update returned false");
    } catch (error) {
        console.error(`Could not update location field ${String(field)}`, error);
        saveError.value = "The location information could not be saved.";
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
        const loaded = await props.location.getLorebook();
        if (requestId === loadRequestId) lorebook.value = loaded;
    } catch (error) {
        if (requestId !== loadRequestId) return;
        console.error("Could not load location lorebook", error);
        loadError.value = "The location lorebook could not be loaded.";
    } finally {
        if (requestId === loadRequestId) loadingLorebook.value = false;
    }
}

onMounted(() => void loadLorebook());

watch(
    [
        () => props.location.get("worldID"),
        () => props.location.get("id"),
    ],
    () => {
        savingFields.value = new Set();
        saveError.value = null;
        void loadLorebook();
    },
);
</script>

<template>
    <article class="location-editor" :aria-busy="isSaving || loadingLorebook">
        <header class="location-editor__header">
            <div>
                <span class="location-editor__eyebrow">Location information</span>
                <h2 class="location-editor__title">{{ locationName }}</h2>
                <p class="location-editor__description">
                    Edit descriptive data for this location. Position, containing region, size, and connection lifecycle remain graph responsibilities.
                </p>
            </div>

        </header>

        <div v-if="saveError" class="location-editor__error" role="alert">
            <span>{{ saveError }}</span>
            <button type="button" @click="saveError = null">Dismiss</button>
        </div>

        <section class="location-editor__section">
            <FieldEditorWrapper field-name="Name" info="The location's display name." :vertical="true">
                <ShortTextBox
                    :model-value="location.get('name')"
                    aria-label="Location name"
                    @edit="value => updateField('name', value)"
                />
            </FieldEditorWrapper>

            <FieldEditorWrapper field-name="Description" info="Narrative and semantic context associated with the location." :vertical="true">
                <LongTextBox
                    :model-value="location.get('description')"
                    aria-label="Location description"
                    tokenize
                    :tokenization-started="true"
                    @edit="value => updateField('description', value)"
                />
            </FieldEditorWrapper>
        </section>

        <section class="location-editor__section location-editor__section--accent">
            <Expandable title="Location lorebook" info="Lore scoped to this location." :initially-open="false">
                <LorebookEditor v-if="lorebook" :model-value="lorebook" />

                <div v-else-if="loadingLorebook" class="location-editor__state" role="status">
                    Loading location lorebook…
                </div>

                <div v-else-if="loadError" class="location-editor__state location-editor__state--error" role="alert">
                    <span>{{ loadError }}</span>
                    <button type="button" @click="loadLorebook">Retry</button>
                </div>

                <div v-else class="location-editor__state">
                    This location does not currently expose a lorebook.
                </div>
            </Expandable>
        </section>

        <section class="location-editor__section location-editor__section--accent">
            <Expandable
                title="Starting characters"
                info="Characters that may begin a session at this location."
                :initially-open="false"
            >
                <LocationStartingCharactersEditor :location="location" />
            </Expandable>
        </section>
    </article>
</template>

<style scoped>
.location-editor {
    display: grid;
    gap: var(--space-5);
    min-width: 0;
    color: rgb(var(--c-fg));
}

.location-editor__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-4);
}

.location-editor__eyebrow {
    color: rgb(var(--c-muted));
    font-size: 0.72rem;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.location-editor__title {
    margin: var(--space-1) 0 0;
    color: rgb(var(--c-fg-strong));
    font-size: 1.35rem;
    overflow-wrap: anywhere;
}

.location-editor__description {
    margin: var(--space-2) 0 0;
    color: rgb(var(--c-muted));
    font-size: 0.9rem;
    line-height: 1.5;
}

.location-editor__status {
    flex: 0 0 auto;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-round);
    background: rgb(var(--c-info-soft));
    color: rgb(var(--c-info-strong));
    font-size: 0.75rem;
    font-weight: 800;
}

.location-editor__section {
    display: grid;
    gap: var(--space-4);
    min-width: 0;
    padding: var(--space-4);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-lg);
    background: rgb(var(--c-surface-raised));
}

.location-editor__section--accent {
    border-color: rgb(var(--c-accent-2) / 0.6);
}

.location-editor__error,
.location-editor__state--error {
    border-color: rgb(var(--c-danger) / 0.5);
    background: rgb(var(--c-danger-soft));
    color: rgb(var(--c-danger-strong));
}

.location-editor__error,
.location-editor__state {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-3);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-md);
}

.location-editor button {
    border: 1px solid currentColor;
    border-radius: var(--radius-sm);
    background: transparent;
    color: inherit;
    cursor: pointer;
}
</style>
