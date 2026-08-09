<script setup lang="ts">
import {computed, ref, watch} from "vue";

import type {EdgeData} from "@/domain/World";
import {Location, LocationEdge} from "@/domain/World";

import BooleanTickBox from "@/components/primitive-editors/BooleanTickBox.vue";
import LongTextBox from "@/components/primitive-editors/LongTextBox.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";

const props = defineProps<{
    source: Location;
    destination: Location;
    edge: LocationEdge;
}>();

const savingFields = ref<ReadonlySet<keyof EdgeData>>(new Set());
const saveError = ref<string | null>(null);

const sourceName = computed(() => locationName(props.source));
const destinationName = computed(() => locationName(props.destination));
const directionLabel = computed(() => `${sourceName.value} → ${destinationName.value}`);
const isSaving = computed(() => savingFields.value.size > 0);

function locationName(location: Location): string {
    const name = String(location.get("name") ?? "").trim();
    return name || "Unnamed location";
}

function markFieldSaving(field: keyof EdgeData, saving: boolean): void {
    const next = new Set(savingFields.value);
    if (saving) next.add(field);
    else next.delete(field);
    savingFields.value = next;
}

async function updateField<K extends keyof EdgeData>(field: K, value: EdgeData[K]): Promise<void> {
    if (savingFields.value.has(field) || props.edge.get(field) === value) return;

    saveError.value = null;
    markFieldSaving(field, true);

    try {
        const updated = await props.edge.update(field, value);
        if (updated === false) throw new Error("Edge update returned false");
    } catch (error) {
        console.error(`Could not update edge field ${String(field)}`, error);
        saveError.value = "The connection information could not be saved.";
    } finally {
        markFieldSaving(field, false);
    }
}

watch(
    () => props.edge.hashKey(),
    () => {
        savingFields.value = new Set();
        saveError.value = null;
    },
);
</script>

<template>
    <article class="edge-editor" :aria-busy="isSaving">
        <header class="edge-editor__header">
            <div>
                <span class="edge-editor__eyebrow">Directed connection</span>
                <h3 class="edge-editor__title">{{ directionLabel }}</h3>
            </div>

        </header>

        <p class="edge-editor__scope">
            This editor changes only descriptive properties of this existing direction. Connection creation and deletion are handled by the graph.
        </p>

        <div v-if="saveError" class="edge-editor__error" role="alert">
            <span>{{ saveError }}</span>
            <button type="button" @click="saveError = null">Dismiss</button>
        </div>

        <div class="edge-editor__fields">
            <FieldEditorWrapper field-name="Description" info="Context attached to this directed connection." :vertical="true">
                <LongTextBox
                    :model-value="edge.get('edge_description')"
                    aria-label="Connection description"
                    tokenize
                    @edit="value => updateField('edge_description', value)"
                />
            </FieldEditorWrapper>

            <div class="edge-editor__toggles">
                <FieldEditorWrapper field-name="Traversable" info="Whether this direction may be used for travel." :vertical="true">
                    <BooleanTickBox
                        :model-value="edge.get('is_traversable')"
                        @edit="value => updateField('is_traversable', value)"
                    />
                </FieldEditorWrapper>

                <FieldEditorWrapper field-name="Show destination name" info="Expose the destination name when presenting this direction." :vertical="true">
                    <BooleanTickBox
                        :model-value="edge.get('show_destination_name')"
                        @edit="value => updateField('show_destination_name', value)"
                    />
                </FieldEditorWrapper>

                <FieldEditorWrapper field-name="Show destination description" info="Expose the destination description when presenting this direction." :vertical="true">
                    <BooleanTickBox
                        :model-value="edge.get('show_destination_description')"
                        @edit="value => updateField('show_destination_description', value)"
                    />
                </FieldEditorWrapper>
            </div>
        </div>
    </article>
</template>

<style scoped>
.edge-editor {
    display: grid;
    gap: var(--space-4);
    min-width: 0;
    padding: var(--space-4);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-lg);
    background: rgb(var(--c-surface-raised));
    color: rgb(var(--c-fg));
    box-shadow: 0 12px 30px rgb(var(--c-shadow) / 0.12);
}

.edge-editor__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-3);
}

.edge-editor__eyebrow {
    color: rgb(var(--c-muted));
    font-size: 0.72rem;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.edge-editor__title {
    margin: var(--space-1) 0 0;
    color: rgb(var(--c-fg-strong));
    font-size: 1rem;
    overflow-wrap: anywhere;
}

.edge-editor__status {
    flex: 0 0 auto;
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-round);
    background: rgb(var(--c-info-soft));
    color: rgb(var(--c-info-strong));
    font-size: 0.75rem;
    font-weight: 800;
}

.edge-editor__scope {
    margin: 0;
    color: rgb(var(--c-muted));
    font-size: 0.86rem;
    line-height: 1.5;
}

.edge-editor__error {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-3);
    border: 1px solid rgb(var(--c-danger) / 0.5);
    border-radius: var(--radius-md);
    background: rgb(var(--c-danger-soft));
    color: rgb(var(--c-danger-strong));
}

.edge-editor__error button {
    border: 1px solid currentColor;
    border-radius: var(--radius-sm);
    background: transparent;
    color: inherit;
    cursor: pointer;
}

.edge-editor__fields,
.edge-editor__toggles {
    display: grid;
    gap: var(--space-4);
    min-width: 0;
}

.edge-editor__toggles {
    grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
}
</style>
