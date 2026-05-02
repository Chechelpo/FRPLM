<script setup lang="ts">
import {ActivationStrategy, Entry, KeyWord, KeywordData} from "@/domain/entities/Lorebook";
import LongTextBox from "@/components/utils/field-editors/LongTextBox.vue";
import {computed, onMounted, ref, watch} from "vue";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";
import SingleEnumInput from "@/components/utils/field-editors/SingleEnumInput.vue";
import AutoCompleteBox from "@/components/utils/AutoCompleteBox.vue";
import {createEntity} from "@/domain/entities/EntityFetch";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";

// ---- model / emit -------------------------------------------------------
const props = defineProps<{
  entry: Entry
}>();
const emit = defineEmits<{
  (e: 'delete', payload: Entry): void;
}>();

// ---- Render state -------------------------------------------------
const expanded = ref<boolean>(false);

function openExpanded() {
  if (!expanded.value) console.info(`Editing entry ${props.entry}`)
  expanded.value = !expanded.value;
}

function onDelete() {
  emit('delete', props.entry);
}

const activationStrategyValues = Object.values(ActivationStrategy)
    .filter((value): value is ActivationStrategy => typeof value === "number");

const activationStrategyLabels: Record<ActivationStrategy, string> = {
  [ActivationStrategy.CONSTANT]: "CONSTANT",
  [ActivationStrategy.COMMON]: "COMMON",
  [ActivationStrategy.EMBEDDING]: "EMBEDDING",
};

// ---- Edit handlers -------------------------------------------------------
async function handleNewKeyword(name:string):Promise<void>{
  let keyword:KeyWord;
  if(!keywordsByName.value?.has(name)){
      keyword = await createEntity<any, KeywordData, KeyWord>(
          null,
          {
            name:name
          },
          EntityTypes.KEYWORD,
          KeyWord
      )
  }else keyword = keywordsByName.value?.get(name)!

  await props.entry.addKeyword(keyword);
}

function handleRemoveKeyword(name:string):void{

}
// ---- values -------------------------------------------------------
const name = computed<string | null>({
  get() {
    return props.entry.get('name')
  },
  set(value) {
    props.entry.update('name', value);
  }
})
const content = computed<string | null>({
  get() {
    return props.entry.get('content')
  },
  set(value) {
    props.entry.update('content', value);
  }
})

// Injection requirements
const probability = computed<number | null>({
  get() {
    return props.entry.get('probability')
  },
  set(value: number | null) {
    props.entry.update('probability', value)
  }
})
const strategy = computed<ActivationStrategy>({
  get() {
    return props.entry.get('strategy')
  },
  set(value: ActivationStrategy) {
    props.entry.update('strategy', value)
  }
})
const isEmbeddingStrategy = computed<boolean>(() => {
  return strategy.value === ActivationStrategy.EMBEDDING;
});
const embed_text = computed<string | null>({
  get() {
    if (isEmbeddingStrategy.value)
      return null
    return props.entry.get('embed_text')
  },
  set(value: string | null) {
    props.entry.update('embed_text', value);
  }
})
const outlet = computed<string | null>({
  get() {
    return props.entry.get('outlet')
  },
  set(value: string | null) {
    props.entry.update('outlet', value);
  }
})

const entryKeywords = ref<KeyWord[]>([]);
const entryKeywordsNames = ref<string[]>([]);

const allKeywords = ref<KeyWord[]>([]);
const keywordsNames = ref<string[]>([]);
const keywordsByName = ref<Map<string, KeyWord>>()

async function load() {
  entryKeywords.value = await props.entry.getKeywords();
  entryKeywordsNames.value = entryKeywords.value.map(keyword => keyword.get('name'));

  allKeywords.value = await KeyWord.getAll();

  keywordsNames.value = allKeywords.value.map(keyword => keyword.get('name'))
  const byName = new Map<string, KeyWord>();

  allKeywords.value.forEach(keyword => {
    byName.set(keyword.get('name'), keyword as KeyWord);
  });

  keywordsByName.value = byName;
}


watch(
    () => props.entry,
    () => {
      load();
    }
)
onMounted(async () => {
  await load()
})
</script>

<template>
  <!-- TITLE Name + expand entry bar -->
  <div class="flexColumnBar">
    <ShortTextBox
        v-model="name"
        @edit="txt => name = txt"
    ></ShortTextBox>
    <button
        class="expandButton"
        type="button"
        @click="openExpanded"
        title="expand"
    >⤢
    </button>
    <button
        type="button"
        @click="onDelete"
    >D
    </button>
  </div>

  <!-- Expanded editor -->
  <div class="expandedTop" v-if="expanded">
    <!--Injection requirements -->
    <!-- Keywords -->
    <AutoCompleteBox
        v-if="!(strategy.valueOf() == ActivationStrategy.CONSTANT)"
        placeholder="Activation keywords"
        :model-value="entryKeywordsNames"
        :suggestions="keywordsNames"
        @add="newValue => handleNewKeyword(newValue)"
        @remove="stringToRemove => handleRemoveKeyword(stringToRemove)"
        :allow-custom="true"
    />
    <!--Injection strategy -->
    <div class=flexColumnBar>
      <div>
        Injection strategy:
        <SingleEnumInput
            :value="strategy!"
            :possible_values="activationStrategyValues"
            :labels="activationStrategyLabels"
            @edit="option => strategy = option"
        ></SingleEnumInput>
      </div>
      <!--Outlet -->
      <div>
        | Outlet:
        <ShortTextBox
            v-model="outlet"
            @edit="txt => outlet = txt"
        > outlet
        </ShortTextBox>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-row">
      <div v-if="isEmbeddingStrategy">
        EmbeddingText (if empty its ignored)
        <LongTextBox
            v-if="isEmbeddingStrategy"
            v-model="embed_text"
            @edit="txt => embed_text = txt"
        />
      </div>
      <div>
        Content:
        <LongTextBox
            v-model="content"
            @edit="txt => content = txt"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.flexColumnBar {
  display: flex;
  flex-direction: row;

  width: 100%;
}

.expandedTop {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.expandButton {
  margin-left: auto;
}
</style>