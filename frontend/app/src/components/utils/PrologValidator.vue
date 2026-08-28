<!-- PrologValidator.vue -->
<script setup lang="ts">
import {     API_BASE,
    fetchApi } from "@frplm/host-sdk";

import {
  computed,
  onBeforeUnmount,
  ref,
  watch,
} from "vue";



type ErrorType =
    | "BAD_SYNTAX"
    | "UNKNOWN_TERM";

/**
 * Exact JSON representation of:
 *
 * PrologSourceValidator.ValidationResult
 */
interface ValidationResult {
  valid: boolean;
  errorType: ErrorType | null;
  message: string | null;
  offendingSymbol: string | null;
  line: number | null;
  column: number | null;
}

const props = withDefaults(
    defineProps<{
      endpoint?: string;
      delayMs?: number;
    }>(),
    {
      endpoint: `${API_BASE}/prolog/validate`,
      delayMs: 500,
    },
);

const source = ref("");
const result = ref<ValidationResult | null>(null);
const requestError = ref<string | null>(null);
const loading = ref(false);

let debounceTimer: ReturnType<typeof setTimeout> | null = null;
let activeRequest: AbortController | null = null;
let requestSequence = 0;

/**
 * Returns the validation result only when the backend reports failure.
 */
const validationError = computed<ValidationResult | null>(() => {
  if (result.value === null || result.value.valid) {
    return null;
  }

  return result.value;
});

/**
 * A result is valid only when the backend explicitly sends valid: true.
 */
const isValid = computed<boolean>(() => {
  return result.value?.valid === true;
});

/**
 * Used by the template to select the edit-box visual variant.
 */
const boxVariant = computed<string>(() => {
  if (requestError.value !== null || validationError.value !== null) {
    return "edit-box--danger";
  }

  if (isValid.value) {
    return "edit-box--success";
  }

  return "edit-box--accent";
});

function cancelDebounce(): void {
  if (debounceTimer === null) {
    return;
  }

  clearTimeout(debounceTimer);
  debounceTimer = null;
}

function cancelActiveRequest(): void {
  if (activeRequest === null) {
    return;
  }

  activeRequest.abort();
  activeRequest = null;
}

function resetValidationState(): void {
  result.value = null;
  requestError.value = null;
}

/**
 * Sends the current Prolog source to the backend.
 */
async function validate(): Promise<void> {
  cancelDebounce();

  const currentSource = source.value;

  if (!currentSource.trim()) {
    cancelActiveRequest();

    resetValidationState();
    loading.value = false;

    return;
  }

  cancelActiveRequest();

  const controller = new AbortController();
  const currentSequence = ++requestSequence;

  activeRequest = controller;
  loading.value = true;
  requestError.value = null;

  try {
    const response = await fetchApi(props.endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        source: currentSource,
      }),
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(
          `Validation request failed with HTTP ${response.status}`,
      );
    }

    const responseResult =
        (await response.json()) as ValidationResult;

    /*
     * Ignore responses belonging to an older request or an older
     * version of the source.
     */
    if (
        currentSequence !== requestSequence ||
        source.value !== currentSource
    ) {
      return;
    }

    result.value = responseResult;
  } catch (error: unknown) {
    if (
        error instanceof DOMException &&
        error.name === "AbortError"
    ) {
      return;
    }

    if (currentSequence !== requestSequence) {
      return;
    }

    requestError.value =
        error instanceof Error
            ? error.message
            : "The validation request failed.";

    result.value = null;
  } finally {
    if (
        activeRequest === controller &&
        currentSequence === requestSequence
    ) {
      activeRequest = null;
      loading.value = false;
    }
  }
}

/**
 * Schedules validation after the user stops typing for delayMs.
 */
function scheduleValidation(): void {
  cancelDebounce();

  if (!source.value.trim()) {
    cancelActiveRequest();

    resetValidationState();
    loading.value = false;

    return;
  }

  debounceTimer = setTimeout(() => {
    debounceTimer = null;
    void validate();
  }, props.delayMs);
}

/**
 * Runs validation immediately, for example from a button or Ctrl+Enter.
 */
function validateImmediately(): void {
  cancelDebounce();
  void validate();
}

/**
 * Clears the editor and all validation state.
 */
function clear(): void {
  cancelDebounce();
  cancelActiveRequest();

  requestSequence++;

  source.value = "";
  result.value = null;
  requestError.value = null;
  loading.value = false;
}

/**
 * Every source modification invalidates the old result and schedules
 * another backend validation.
 */
watch(source, () => {
  cancelActiveRequest();

  requestSequence++;
  loading.value = false;

  resetValidationState();
  scheduleValidation();
});

onBeforeUnmount(() => {
  cancelDebounce();
  cancelActiveRequest();
});
</script>
<template>
  <section class="edit-box" :class="boxVariant">
    <header class="edit-box__header">
      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">Prolog</span>

        <div class="edit-box__title-row">
          <h2 class="edit-box__title">
            Syntax validator
          </h2>

          <span
              v-if="loading"
              class="edit-box__badge edit-box__badge--neutral"
          >
            Validating
          </span>

          <span
              v-else-if="isValid"
              class="edit-box__badge edit-box__badge--success"
          >
            Valid
          </span>

          <span
              v-else-if="validationError"
              class="edit-box__badge edit-box__badge--danger"
          >
            Invalid
          </span>
        </div>

        <p class="edit-box__description">
          Validation runs {{ delayMs }} ms after the last edit.
        </p>
      </div>
    </header>

    <div class="edit-box__body">
      <textarea
          v-model="source"
          class="prolog-validator__source"
          placeholder="parent(alice, bob)."
          spellcheck="false"
          @keydown.ctrl.enter.prevent="validateImmediately"
          @keydown.meta.enter.prevent="validateImmediately"
      />

      <div
          v-if="loading"
          class="edit-box__state prolog-validator__result"
      >
        <span class="edit-box__spinner" />

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Validating source
          </strong>
        </div>
      </div>

      <div
          v-else-if="requestError"
          class="
          edit-box__state
          edit-box__state--error
          prolog-validator__result
        "
      >
        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Request failed
          </strong>

          <p class="edit-box__state-description">
            {{ requestError }}
          </p>
        </div>
      </div>

      <div
          v-else-if="isValid"
          class="
          edit-box__state
          edit-box__state--success
          prolog-validator__result
        "
      >
        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Valid Prolog
          </strong>

          <p class="edit-box__state-description">
            No syntax errors were found.
          </p>
        </div>
      </div>

      <div
          v-else-if="validationError"
          class="
          edit-box__state
          edit-box__state--error
          prolog-validator__result
        "
      >
        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            {{ validationError.type }}
          </strong>

          <p class="edit-box__state-description">
            {{ validationError.message }}
          </p>

          <dl class="prolog-validator__details">
            <dt>Position</dt>
            <dd>
              Line {{ validationError.line }},
              column {{ validationError.column }}
            </dd>

            <template v-if="validationError.offendingSymbol">
              <dt>Symbol</dt>
              <dd>
                <code>
                  {{ validationError.offendingSymbol }}
                </code>
              </dd>
            </template>
          </dl>
        </div>
      </div>
    </div>

    <footer class="edit-box__footer">
      <button
          type="button"
          class="edit-box__action"
          :disabled="!source"
          @click="clear"
      >
        Clear
      </button>

      <button
          type="button"
          class="edit-box__action edit-box__action--accent"
          :disabled="loading || !source.trim()"
          @click="validateImmediately"
      >
        Validate now
      </button>
    </footer>
  </section>
</template>

<style scoped>
.prolog-validator__source {
  display: block;
  width: 100%;
  min-height: 16rem;
  box-sizing: border-box;
  resize: vertical;

  padding: var(--space-4);

  color: rgb(var(--c-fg-strong));
  background: rgb(var(--c-surface-raised) / 0.7);

  border: 1px solid rgb(var(--c-border) / 0.35);
  border-radius: var(--radius-md);

  font-family: var(--font-mono, monospace);
  font-size: 0.9rem;
  line-height: 1.6;
  tab-size: 4;

  outline: none;
}

.prolog-validator__source:focus {
  border-color: rgb(var(--edit-box-accent) / 0.65);

  box-shadow:
      0 0 0 var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.13);
}

.prolog-validator__result {
  min-height: auto;
  justify-content: flex-start;

  margin-top: var(--space-3);
  padding: var(--space-4);

  text-align: left;
}

.prolog-validator__details {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: var(--space-1) var(--space-3);

  margin: var(--space-3) 0 0;

  font-size: 0.78rem;
}

.prolog-validator__details dt {
  font-weight: 800;
}

.prolog-validator__details dd {
  margin: 0;
}
</style>