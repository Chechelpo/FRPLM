## Component: `TagAutocomplete.vue`

A domain‑specific wrapper around `AutoCompleteBox`. It fetches the available `Tag` entities from your API and feeds them as suggestions, while exposing a clean `v-model` interface and custom events for the parent.

### Props

| Prop          | Type       | Default | Description                                                                 |
|---------------|------------|---------|-----------------------------------------------------------------------------|
| `modelValue`   | `string[]` | `[]`    | Initial (or current) selected tag names. Works with `v-model` on the parent.|

### Emits

| Event              | Payload    | Description                                                                                     |
|--------------------|------------|-------------------------------------------------------------------------------------------------|
| `update:modelValue` | `string[]` | Emitted when the selected‑tags array changes (standard `v-model` binding).                      |
| `newTag`           | `string`   | Emitted when a tag is added (for convenience, independent of `v-model`).                        |
| `removeTag`         | `string`   | Emitted when a tag is removed (for convenience, independent of `v-model`).                      |
|
### Internal logic

- On `onMounted`, it calls `fetch_all(EntityTypes.TAGS, Tag)` to obtain all `Tag` instances.
- The tag names are extracted and stored in a local reactive array that is passed as `suggestions` to `AutoCompleteBox`.
- The component binds `v-model` to `AutoCompleteBox` so that the selected tag list stays synchronised.
- It forwards the `add` and `remove` events from `AutoCompleteBox` as `newTag` and `removeTag` respectively, allowing the parent to react to individual changes without inspecting the array.

### Usage

```vue
TagBox
v-model="myTags"
@newTag="(tag) => console.log('Added', tag)"
@removeTag="(tag) => console.log('Removed', tag)"
/>
```

This approach separates the generic autocomplete logic from the business logic of loading the tag suggestions, keeping both parts clean and reusable.
```

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createEntity, fetch_all } from '@/domain/entities/EntityFetch'
import { Tag, TagData, TagKey } from '@/domain/entities/tags/Tag'
import { EntityTypes } from '@/frameworks/entities/EntityTypes'
import AutoCompleteBox from '@/components/utils/AutoCompleteBox.vue'

const model = defineModel<Tag[]>({
  default: () => []
})

const emit = defineEmits<{
  (e: 'newTag', tag: Tag): void
  (e: 'newCustomTag', tag: Tag): void
  (e: 'removeTag', tag: Tag): void
}>()

const tagsByName = ref<Map<string, Tag>>(new Map())
const knownNames = ref<string[]>([])

const selectedNames = computed<string[]>({
  get() {
    return model.value.map(tag => String(tag.get('name')))
  },

  set(names: string[]) {
    const knownByName = tagsByName.value

    const currentByName = new Map<string, Tag>(
        model.value.map(tag => [String(tag.get('name')), tag])
    )

    model.value = names
        .map(name => knownByName.get(name) ?? currentByName.get(name))
        .filter((tag): tag is Tag => tag !== undefined)
  }
})

async function handleAdd(tagName: string) {
  if (!tagName) return

  const existing = tagsByName.value.get(tagName)!

  if (existing !== undefined) {
    emit('newTag', existing)
    return
  }

  try {
    const newTag = await createEntity<TagKey, TagData, Tag>(
        null,
        { name: tagName } as TagData,
        EntityTypes.TAGS,
        Tag
    )

    tagsByName.value.set(newTag.get('name'), newTag)
    knownNames.value = [...tagsByName.value.keys()]

    model.value = [...model.value, newTag]

    emit('newTag', newTag)
    emit('newCustomTag', newTag)
  } catch (err) {
    console.error(`Failed to create tag "${tagName}"`, err)
  }
}

function handleRemove(tagName: string) {
  const tag = tagsByName.value.get(tagName)
  if (!tag) return

  emit('removeTag', tag)
}

onMounted(async () => {
  const allTags = await fetch_all<TagKey, TagData, Tag>(
      EntityTypes.TAGS,
      Tag
  )

  const map = new Map<string, Tag>()

  for (const tag of allTags) {
    map.set(tag.get('name'), tag)
  }

  tagsByName.value = map
  knownNames.value = [...map.keys()]
})
</script>

<template>
  <AutoCompleteBox
      v-model="selectedNames"
      :suggestions="knownNames"
      :allow-custom="true"
      @add="handleAdd"
      @remove="handleRemove"
  />
</template>