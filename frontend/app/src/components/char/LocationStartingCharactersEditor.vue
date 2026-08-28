<script setup lang="ts">
import {     Character,
    Location } from "@frplm/host-sdk";

import {computed, onMounted, ref, shallowRef, watch} from "vue";


import CharacterEditor from "@components/char/CharacterEditor.vue";

const props = defineProps<{
    location: Location;
}>();

const characters = shallowRef<Character[]>([]);
const startingCharacterIds = ref<ReadonlySet<number>>(new Set());
const busyCharacterIds = ref<ReadonlySet<number>>(new Set());
const editingCharacter = shallowRef<Character | null>(null);

const loading = ref(false);
const creating = ref(false);
const newCharacterName = ref("");
const loadError = ref<string | null>(null);
const operationError = ref<string | null>(null);

let loadRequestId = 0;

const locationName = computed(() => {
    const name = String(props.location.get("name") ?? "").trim();
    return name || "Unnamed location";
});

const sortedCharacters = computed(() =>
    [...characters.value].sort((left, right) =>
        left.get("name").localeCompare(right.get("name"), undefined, {
            sensitivity: "base",
        }),
    ),
);

function replaceSetValue<T>(
    source: ReadonlySet<T>,
    value: T,
    present: boolean,
): ReadonlySet<T> {
    const next = new Set(source);
    if (present) next.add(value);
    else next.delete(value);
    return next;
}

function startsHere(character: Character): boolean {
    return startingCharacterIds.value.has(character.get("id"));
}

function isBusy(character: Character): boolean {
    return busyCharacterIds.value.has(character.get("id"));
}

async function loadCharacters(): Promise<void> {
    const requestId = ++loadRequestId;
    loading.value = true;
    loadError.value = null;
    operationError.value = null;

    try {
        const loadedCharacters = await Character.getAll();
        const memberships = await Promise.all(
            loadedCharacters.map(async character => ({
                character,
                locations: await character.getStartingLocation(),
            })),
        );

        if (requestId !== loadRequestId) return;

        characters.value = loadedCharacters;
        startingCharacterIds.value = new Set(
            memberships
                .filter(({locations}) =>
                    locations.some(location => location.equals(props.location)),
                )
                .map(({character}) => character.get("id")),
        );
    } catch (error) {
        if (requestId !== loadRequestId) return;
        console.error("Could not load characters for starting-location editor", error);
        loadError.value = "Characters and their starting locations could not be loaded.";
    } finally {
        if (requestId === loadRequestId) loading.value = false;
    }
}

async function toggleStartingLocation(character: Character): Promise<void> {
    const characterId = character.get("id");
    if (busyCharacterIds.value.has(characterId)) return;

    const wasStartingHere = startsHere(character);
    operationError.value = null;
    busyCharacterIds.value = replaceSetValue(
        busyCharacterIds.value,
        characterId,
        true,
    );

    try {
        if (wasStartingHere) {
            await character.unmarkStartingAt(props.location);
        } else {
            await character.markStartingAt(props.location);
        }

        startingCharacterIds.value = replaceSetValue(
            startingCharacterIds.value,
            characterId,
            !wasStartingHere,
        );
    } catch (error) {
        console.error("Could not update character starting location", error);
        operationError.value = `Could not update the starting location for ${character.get("name") || "the character"}.`;
    } finally {
        busyCharacterIds.value = replaceSetValue(
            busyCharacterIds.value,
            characterId,
            false,
        );
    }
}

async function createCharacter(): Promise<void> {
    const name = newCharacterName.value.trim();
    if (!name || creating.value) return;

    creating.value = true;
    operationError.value = null;

    let character: Character | null = null;

    try {
        character = await props.location.createCharacterStartingHere(name);
        characters.value = [...characters.value, character];
        newCharacterName.value = "";

        startingCharacterIds.value = replaceSetValue(
            startingCharacterIds.value,
            character.get("id"),
            true,
        );

        editingCharacter.value = character;
    } catch (error) {
        console.error("Could not create character at this starting location", error);
        operationError.value = character
            ? `Character ${character.get("name")} was created, but could not be assigned to this starting location.`
            : "The character could not be created.";
    } finally {
        creating.value = false;
    }
}

function editCharacter(character: Character): void {
    operationError.value = null;
    editingCharacter.value = character;
}

function returnToLocation(): void {
    editingCharacter.value = null;
}

onMounted(() => void loadCharacters());

watch(
    [
        () => props.location.get("worldID"),
        () => props.location.get("id"),
    ],
    () => {
        editingCharacter.value = null;
        startingCharacterIds.value = new Set();
        busyCharacterIds.value = new Set();
        void loadCharacters();
    },
);
</script>

<template>
    <div class="starting-characters">
        <CharacterEditor
            v-if="editingCharacter"
            v-model="editingCharacter"
            show-back-button
            @back="returnToLocation"
        />

        <template v-else>
            <header class="starting-characters__header">
                <div>
                    <h3>Starting characters</h3>
                    <p>
                        Choose which characters may begin at {{ locationName }},
                        or create a character already assigned here.
                    </p>
                </div>

                <button
                    type="button"
                    class="starting-characters__secondary"
                    :disabled="loading"
                    @click="loadCharacters"
                >
                    {{ loading ? "Loading…" : "Reload" }}
                </button>
            </header>

            <form class="starting-characters__create" @submit.prevent="createCharacter">
                <label for="starting-character-name">New character</label>
                <div class="starting-characters__create-row">
                    <input
                        id="starting-character-name"
                        v-model="newCharacterName"
                        type="text"
                        autocomplete="off"
                        placeholder="Character name"
                        :disabled="creating"
                    >
                    <button
                        type="submit"
                        class="starting-characters__primary"
                        :disabled="creating || !newCharacterName.trim()"
                    >
                        {{ creating ? "Creating…" : "Create and edit" }}
                    </button>
                </div>
            </form>

            <div v-if="operationError" class="starting-characters__error" role="alert">
                <span>{{ operationError }}</span>
                <button type="button" @click="operationError = null">Dismiss</button>
            </div>

            <div v-if="loadError" class="starting-characters__state starting-characters__error" role="alert">
                <span>{{ loadError }}</span>
                <button type="button" @click="loadCharacters">Retry</button>
            </div>

            <div v-else-if="loading" class="starting-characters__state" role="status">
                Loading characters and starting locations…
            </div>

            <div v-else-if="sortedCharacters.length === 0" class="starting-characters__state">
                No characters exist yet. Create the first one above.
            </div>

            <ul v-else class="starting-characters__list">
                <li
                    v-for="character in sortedCharacters"
                    :key="character.get('id')"
                    class="starting-characters__item"
                >
                    <div class="starting-characters__identity">
                        <strong>{{ character.get("name") || "Unnamed character" }}</strong>
                        <span>{{ startsHere(character) ? "Starts here" : "Does not start here" }}</span>
                    </div>

                    <div class="starting-characters__actions">
                        <button
                            type="button"
                            class="starting-characters__secondary"
                            @click="editCharacter(character)"
                        >
                            Edit character
                        </button>
                        <button
                            type="button"
                            class="starting-characters__assignment"
                            :class="{'starting-characters__assignment--active': startsHere(character)}"
                            :aria-pressed="startsHere(character)"
                            :disabled="isBusy(character)"
                            @click="toggleStartingLocation(character)"
                        >
                            {{
                                isBusy(character)
                                    ? "Saving…"
                                    : startsHere(character)
                                        ? "Remove from here"
                                        : "Start here"
                            }}
                        </button>
                    </div>
                </li>
            </ul>
        </template>
    </div>
</template>

<style scoped>
.starting-characters {
    display: grid;
    gap: var(--space-4);
    min-width: 0;
}

.starting-characters__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--space-3);
}

.starting-characters__header h3,
.starting-characters__header p {
    margin: 0;
}

.starting-characters__header h3 {
    color: rgb(var(--c-fg-strong));
    font-size: 1rem;
}

.starting-characters__header p {
    margin-top: var(--space-1);
    color: rgb(var(--c-muted));
    font-size: 0.85rem;
    line-height: 1.45;
}

.starting-characters__create {
    display: grid;
    gap: var(--space-2);
    padding: var(--space-3);
    border: 1px solid rgb(var(--c-accent-2) / 0.45);
    border-radius: var(--radius-md);
    background: rgb(var(--c-surface-2) / 0.42);
}

.starting-characters__create label {
    color: rgb(var(--c-fg-strong));
    font-size: 0.8rem;
    font-weight: 800;
}

.starting-characters__create-row {
    display: flex;
    gap: var(--space-2);
}

.starting-characters__create input {
    flex: 1 1 auto;
    min-width: 0;
    padding: var(--space-2) var(--space-3);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-sm);
    outline: none;
    background: rgb(var(--c-surface-raised));
    color: rgb(var(--c-fg));
    font: inherit;
}

.starting-characters__create input:focus {
    border-color: rgb(var(--c-accent));
    box-shadow: 0 0 0 3px rgb(var(--c-accent) / 0.12);
}

.starting-characters button {
    padding: var(--space-2) var(--space-3);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-sm);
    font: inherit;
    font-size: 0.82rem;
    font-weight: 750;
    cursor: pointer;
}

.starting-characters button:disabled {
    cursor: wait;
    opacity: 0.6;
}

.starting-characters__primary {
    border-color: rgb(var(--c-accent)) !important;
    background: rgb(var(--c-accent)) !important;
    color: rgb(var(--c-page)) !important;
}

.starting-characters__secondary {
    background: transparent;
    color: rgb(var(--c-fg));
}

.starting-characters__state,
.starting-characters__error {
    padding: var(--space-3);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-md);
    color: rgb(var(--c-muted));
}

.starting-characters__error {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    border-color: rgb(var(--c-danger) / 0.5);
    background: rgb(var(--c-danger-soft));
    color: rgb(var(--c-danger-strong));
}

.starting-characters__list {
    display: grid;
    gap: var(--space-2);
    margin: 0;
    padding: 0;
    list-style: none;
}

.starting-characters__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-3);
    border: 1px solid rgb(var(--c-border));
    border-radius: var(--radius-md);
    background: rgb(var(--c-surface-raised));
}

.starting-characters__identity {
    display: grid;
    gap: var(--space-1);
    min-width: 0;
}

.starting-characters__identity strong {
    overflow-wrap: anywhere;
    color: rgb(var(--c-fg-strong));
}

.starting-characters__identity span {
    color: rgb(var(--c-muted));
    font-size: 0.76rem;
}

.starting-characters__actions {
    display: flex;
    flex: 0 0 auto;
    gap: var(--space-2);
}

.starting-characters__assignment {
    background: rgb(var(--c-primary) / 0.08);
    color: rgb(var(--c-fg));
}

.starting-characters__assignment--active {
    border-color: rgb(var(--c-success) / 0.6) !important;
    background: rgb(var(--c-success-soft));
    color: rgb(var(--c-success-strong));
}

@media (max-width: 720px) {
    .starting-characters__header,
    .starting-characters__item,
    .starting-characters__create-row {
        align-items: stretch;
        flex-direction: column;
    }

    .starting-characters__actions {
        flex-wrap: wrap;
    }
}
</style>
