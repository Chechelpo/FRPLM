<script setup lang="ts">
import {ref} from "vue";

const props = defineProps<{
    modelValue: string;
    pending: boolean;
    message: string | null;
    messageKind: "success" | "error" | "neutral";
}>();

const emit = defineEmits<{
    (event: "update:modelValue", value: string): void;
    (event: "search"): void;
    (event: "clear"): void;
}>();

const inputRef = ref<HTMLInputElement | null>(null);

function focusInput(): void {
    inputRef.value?.focus();
    inputRef.value?.select();
}

defineExpose({
    focusInput,
});

function onInput(event: Event): void {
    emit(
        "update:modelValue",
        (event.currentTarget as HTMLInputElement).value,
    );
}

function onEscape(event: KeyboardEvent): void {
    event.stopPropagation();

    if (props.modelValue.length > 0 || props.message !== null) {
        emit("clear");
    }
}
</script>

<template>
    <div class="world-graph-search" data-graph-control @pointerdown.stop @keydown.stop>
        <form class="world-graph-search__form" role="search" @submit.prevent="emit('search')">
            <svg class="world-graph-search__icon" viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <path d="m16.5 16.5 4 4" />
            </svg>

            <input
                ref="inputRef"
                class="world-graph-search__input"
                type="search"
                :value="modelValue"
                autocomplete="off"
                spellcheck="false"
                aria-label="Search world graph"
                placeholder="location:name, region:name, character:name"
                @input="onInput"
                @keydown.esc.prevent="onEscape"
            >

            <button
                type="submit"
                class="world-graph-search__submit"
                :disabled="pending || modelValue.trim().length === 0"
            >
                {{ pending ? "Searching…" : "Search" }}
            </button>

            <button
                v-if="modelValue.length > 0 || message !== null"
                type="button"
                class="world-graph-search__clear"
                aria-label="Clear graph search"
                title="Clear search"
                @click="emit('clear')"
            >
                ×
            </button>
        </form>

        <p
            v-if="message !== null"
            class="world-graph-search__message"
            :class="`is-${messageKind}`"
            aria-live="polite"
        >
            {{ message }}
        </p>
    </div>
</template>

<style scoped>
.world-graph-search {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;

    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-md);
    background: rgb(var(--c-surface-raised) / 0.96);
    box-shadow: 0 5px 18px rgb(var(--c-shadow) / 0.18);
}

.world-graph-search__form {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto auto;
    align-items: center;
    gap: var(--space-1);

    min-width: 0;
    padding: var(--space-1);
}

.world-graph-search__icon {
    width: 1rem;
    height: 1rem;
    margin-inline: var(--space-1);

    fill: none;
    stroke: rgb(var(--c-muted));
    stroke-width: 2;
    stroke-linecap: round;
}

.world-graph-search__input {
    width: 100%;
    min-width: 0;
    height: 2rem;
    box-sizing: border-box;

    padding: 0 var(--space-2);

    color: rgb(var(--c-fg));
    background: rgb(var(--c-surface));
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-sm);
    font: 0.78rem/1 var(--font-primary);
}

.world-graph-search__input:focus {
    outline: none;
    border-color: rgb(var(--c-border-hover));
    box-shadow: 0 0 0 3px rgb(var(--focus-ring-color) / 0.28);
}

.world-graph-search__submit,
.world-graph-search__clear {
    height: 2rem;

    color: rgb(var(--c-fg));
    background: rgb(var(--c-surface));
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-sm);
    font: 700 0.72rem/1 var(--font-primary);
    cursor: pointer;
}

.world-graph-search__submit {
    min-width: 4.5rem;
    padding: 0 var(--space-2);
}

.world-graph-search__clear {
    width: 2rem;
    padding: 0;
    font-size: 1rem;
}

.world-graph-search__submit:hover:not(:disabled),
.world-graph-search__clear:hover:not(:disabled) {
    background: rgb(var(--c-surface-hover));
    border-color: rgb(var(--c-border-hover));
}

.world-graph-search button:disabled,
.world-graph-search input:disabled {
    opacity: 0.58;
    cursor: not-allowed;
}

.world-graph-search__message {
    margin: 0;
    padding: 0.34rem var(--space-2) 0.42rem;

    color: rgb(var(--c-muted));
    border-top: 1px solid rgb(var(--c-border) / 0.45);
    font-size: 0.7rem;
    line-height: 1.35;
}

.world-graph-search__message.is-success {
    color: rgb(var(--c-success-strong));
}

.world-graph-search__message.is-error {
    color: rgb(var(--c-danger-strong));
}

@container (max-width: 22rem) {
    .world-graph-search__form {
        grid-template-columns: auto minmax(0, 1fr) auto;
    }

    .world-graph-search__submit {
        grid-column: 2 / -1;
        width: 100%;
    }
}
</style>
