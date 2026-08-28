<script setup lang="ts">
import {
    computed,
    nextTick,
    ref,
    watch,
} from "vue";
import {
    nextWorldGraphSearchResultIndex,
    type WorldGraphSearchResult,
} from "./utils/search";

const props = defineProps<{
    modelValue: string;
    pending: boolean;
    message: string | null;
    messageKind: "success" | "error" | "neutral";
    results: readonly WorldGraphSearchResult[];
    matchCount: number | null;
    maxHeight: number;
}>();

const emit = defineEmits<{
    (event: "update:modelValue", value: string): void;
    (event: "search"): void;
    (event: "choose", resultKey: string): void;
    (event: "clear"): void;
}>();

const inputRef = ref<HTMLInputElement | null>(null);
const resultListRef = ref<HTMLElement | null>(null);
const activeResultIndex = ref(0);
const componentId = `world-graph-search-${Math.random()
    .toString(36)
    .slice(2, 10)}`;
const resultListId = `${componentId}-results`;

const resultsVisible = computed(() => (
    props.results.length > 1
));

const activeResult = computed<WorldGraphSearchResult | null>(() => (
    resultsVisible.value
        ? props.results[activeResultIndex.value] ?? null
        : null
));

const activeOptionId = computed<string | undefined>(() => (
    activeResult.value === null
        ? undefined
        : `${componentId}-option-${activeResultIndex.value}`
));

const matchCountLabel = computed(() => {
    if (props.matchCount === null) return "";
    return `${props.matchCount} ${props.matchCount === 1 ? "match" : "matches"}`;
});

watch(
    () => props.results.map(result => result.key).join("\u0000"),
    () => {
        activeResultIndex.value = 0;
        void scrollActiveResultIntoView();
    },
    {immediate: true},
);

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

async function scrollActiveResultIntoView(): Promise<void> {
    await nextTick();
    const active = resultListRef.value?.querySelector<HTMLElement>(
        `[data-search-result-index="${activeResultIndex.value}"]`,
    );
    active?.scrollIntoView({block: "nearest"});
}

function moveActiveResult(direction: -1 | 1): void {
    activeResultIndex.value = nextWorldGraphSearchResultIndex(
        activeResultIndex.value,
        props.results.length,
        direction,
    );
    void scrollActiveResultIntoView();
}

function chooseResult(result: WorldGraphSearchResult): void {
    emit("choose", result.key);
    void nextTick(focusInput);
}

function chooseActiveResult(): void {
    const result = activeResult.value;
    if (result !== null) chooseResult(result);
}

function onInputKeyDown(event: KeyboardEvent): void {
    if (event.key === "Escape") {
        event.preventDefault();
        event.stopPropagation();

        if (
            props.modelValue.length > 0 ||
            props.message !== null ||
            props.matchCount !== null
        ) {
            emit("clear");
        }
        return;
    }

    if (!resultsVisible.value) return;

    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
        event.preventDefault();
        event.stopPropagation();
        moveActiveResult(event.key === "ArrowDown" ? 1 : -1);
        return;
    }

    if (event.key === "Enter") {
        event.preventDefault();
        event.stopPropagation();
        chooseActiveResult();
    }
}

function setActiveResult(index: number): void {
    if (index < 0 || index >= props.results.length) return;
    activeResultIndex.value = index;
}
</script>

<template>
    <div
        class="world-graph-search"
        :style="{maxHeight: `${Math.max(1, maxHeight)}px`}"
        data-graph-control
        @pointerdown.stop
        @keydown.stop
    >
        <section
            v-if="resultsVisible"
            :id="resultListId"
            ref="resultListRef"
            class="world-graph-search__results"
            role="listbox"
            aria-label="Matching graph entities"
        >
            <header class="world-graph-search__results-heading">
                <strong>{{ matchCountLabel }}</strong>
                <span>↑/↓ choose · Enter apply</span>
            </header>

            <button
                v-for="(result, index) in results"
                :id="`${componentId}-option-${index}`"
                :key="result.key"
                type="button"
                class="world-graph-search__result"
                :class="{'is-active': index === activeResultIndex}"
                :data-search-result-index="index"
                role="option"
                :aria-selected="index === activeResultIndex"
                tabindex="-1"
                @pointerdown.prevent
                @pointermove="setActiveResult(index)"
                @click="chooseResult(result)"
            >
                <span class="world-graph-search__result-main">
                    <strong>{{ result.name || "(unnamed)" }}</strong>
                    <span>{{ result.kind }}</span>
                </span>
                <small>{{ result.context }}</small>
            </button>
        </section>

        <form
            class="world-graph-search__form"
            role="search"
            @submit.prevent="emit('search')"
        >
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
                role="combobox"
                aria-autocomplete="list"
                aria-haspopup="listbox"
                aria-label="Search world graph"
                :aria-busy="pending"
                :aria-expanded="resultsVisible"
                :aria-controls="resultsVisible ? resultListId : undefined"
                :aria-activedescendant="activeOptionId"
                placeholder="location:name, region:name, character:name"
                @input="onInput"
                @keydown="onInputKeyDown"
            >

            <output
                v-if="matchCount !== null"
                class="world-graph-search__count"
                aria-live="polite"
            >{{ matchCountLabel }}</output>

            <button
                type="submit"
                class="world-graph-search__submit"
                :disabled="pending || modelValue.trim().length === 0"
            >
                {{ pending ? "Searching…" : "Search" }}
            </button>

            <button
                v-if="modelValue.length > 0 || message !== null || matchCount !== null"
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
    display: flex;
    flex-direction: column;

    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
    overflow: hidden;

    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-md);
    background: rgb(var(--c-surface-raised) / 0.96);
    box-shadow: 0 5px 18px rgb(var(--c-shadow) / 0.18);
}

.world-graph-search__results {
    flex: 1 1 auto;
    min-height: 0;
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;

    border-bottom: 1px solid rgb(var(--c-border));
    scrollbar-gutter: stable;
}

.world-graph-search__results-heading {
    position: sticky;
    z-index: 1;
    top: 0;

    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);

    padding: 0.4rem var(--space-2);

    color: rgb(var(--c-muted));
    background: rgb(var(--c-surface-raised));
    border-bottom: 1px solid rgb(var(--c-border) / 0.55);
    font: 0.68rem/1.2 var(--font-primary);
}

.world-graph-search__results-heading strong {
    color: rgb(var(--c-fg-strong));
    font-weight: 800;
}

.world-graph-search__results-heading span {
    white-space: nowrap;
}

.world-graph-search__result {
    display: grid;
    gap: 0.18rem;

    width: 100%;
    min-width: 0;
    padding: 0.48rem var(--space-2);

    color: rgb(var(--c-fg));
    text-align: left;
    background: transparent;
    border: 0;
    border-bottom: 1px solid rgb(var(--c-border) / 0.36);
    font-family: var(--font-primary);
    cursor: pointer;
}

.world-graph-search__result:last-child {
    border-bottom: 0;
}

.world-graph-search__result:hover,
.world-graph-search__result.is-active {
    background: rgb(var(--c-surface-hover));
}

.world-graph-search__result.is-active {
    box-shadow: inset 3px 0 0 rgb(var(--c-info));
}

.world-graph-search__result-main {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--space-2);
    min-width: 0;
}

.world-graph-search__result-main strong {
    min-width: 0;
    overflow: hidden;
    color: rgb(var(--c-fg-strong));
    font-size: 0.76rem;
    font-weight: 760;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.world-graph-search__result-main span {
    flex: none;
    color: rgb(var(--c-muted));
    font-size: 0.62rem;
    font-weight: 760;
    letter-spacing: 0.04em;
    text-transform: uppercase;
}

.world-graph-search__result small {
    min-width: 0;
    overflow: hidden;
    color: rgb(var(--c-muted));
    font-size: 0.66rem;
    line-height: 1.3;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.world-graph-search__form {
    display: grid;
    flex: none;
    grid-template-columns: auto minmax(0, 1fr) auto auto auto;
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

.world-graph-search__count {
    min-width: 3.9rem;
    color: rgb(var(--c-muted));
    font: 700 0.66rem/1 var(--font-primary);
    text-align: center;
    white-space: nowrap;
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
    flex: none;
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

@container (max-width: 24rem) {
    .world-graph-search__form {
        grid-template-columns: auto minmax(0, 1fr) auto auto;
    }

    .world-graph-search__count {
        grid-column: 1;
        grid-row: 2;
        min-width: 0;
    }

    .world-graph-search__submit {
        grid-column: 2 / 4;
        width: 100%;
    }

    .world-graph-search__clear {
        grid-column: 4;
        grid-row: 1 / 3;
    }
}
</style>
