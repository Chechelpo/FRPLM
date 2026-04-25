<template>
  <div
      class="h-full w-full flex flex-col"
  >
    <div class="grid gap-6 lg:grid-cols-[2fr,3fr]">
      <!-- LEFT: create chat + list of chats -->
      <section class="space-y-4">
        <!-- Error -->
        <div
            v-if="error"
            class="rounded-lg border border-red-700/70 bg-red-900/40 px-3 py-2 text-sm text-red-200"
        >
          {{ error }}
        </div>

        <!-- Create new chat via IDs -->
        <div
            class="rounded-2xl border border-amber-800/60 bg-stone-950/80 p-4 shadow-[0_0_30px_rgba(0,0,0,0.6)]"
        >
          <h2 class="text-lg font-semibold text-amber-300 mb-1">
            Forge a new chat
          </h2>
          <p class="text-xs text-amber-200/70 mb-3">
            Enter the persona ID and NPC ID from the ledger to start a Single chat.
          </p>

          <div class="space-y-3">
            <div class="grid grid-cols-2 gap-3">
              <!-- Persona ID -->
              <div>
                <label class="block text-xs font-semibold text-amber-200 mb-1">
                  Persona ID
                </label>
                <input
                    v-model.number="personaIdInput"
                    type="number"
                    min="1"
                    class="w-full rounded-md border border-amber-700/60 bg-stone-900/80 px-3 py-1.5 text-sm text-amber-100 focus:outline-none focus:ring-2 focus:ring-amber-400/70 focus:border-amber-300/80"
                    placeholder="e.g. 1"
                />
              </div>

              <!-- NPC ID -->
              <div>
                <label class="block text-xs font-semibold text-amber-200 mb-1">
                  NPC ID
                </label>
                <input
                    v-model.number="npcIdInput"
                    type="number"
                    min="1"
                    class="w-full rounded-md border border-amber-700/60 bg-stone-900/80 px-3 py-1.5 text-sm text-amber-100 focus:outline-none focus:ring-2 focus:ring-amber-400/70 focus:border-amber-300/80"
                    placeholder="e.g. 2"
                />
              </div>
            </div>

            <div class="flex items-center justify-between pt-1">
              <span class="text-xs text-amber-300/70">
                Calls <code>/api/chat/createSingle</code>.
              </span>
              <button
                  type="button"
                  class="rounded-md bg-amber-600 px-4 py-1.5 text-sm font-semibold text-stone-950 shadow-md shadow-amber-900/50 transition hover:bg-amber-500 hover:shadow-lg hover:shadow-amber-900/60 disabled:opacity-40 disabled:hover:shadow-none"
                  :disabled="!personaIdInput || !npcIdInput || creating"
                  @click="createSingleChat"
              >
                {{ creating ? 'Creating...' : 'Create chat' }}
              </button>
            </div>

            <p v-if="createMessage" class="text-xs text-emerald-300/80">
              {{ createMessage }}
            </p>
          </div>
        </div>

        <!-- Existing chats list -->
        <div
            class="rounded-2xl border border-amber-800/60 bg-stone-950/80 p-4 shadow-[0_0_30px_rgba(0,0,0,0.6)]"
        >
          <div class="flex items-center justify-between mb-2">
            <h2 class="text-lg font-semibold text-amber-300">
              Tavern conversations
            </h2>
            <button
                type="button"
                class="text-xs text-amber-300/80 hover:text-amber-100"
                @click="refreshSingles"
            >
              Refresh
            </button>
          </div>

          <div v-if="loadingSingles" class="text-xs text-amber-300/80">
            Fetching chats from the register...
          </div>

          <div v-else-if="singles.length === 0" class="text-xs text-amber-300/70">
            No chats yet. Forge one above.
          </div>

          <ul v-else class="space-y-2 max-h-[22rem] overflow-y-auto pr-1">
            <li
                v-for="single in singles"
                :key="single.chatID"
            >
              <button
                  type="button"
                  class="w-full text-left rounded-lg border px-3 py-2 text-sm transition flex flex-col gap-0.5"
                  :class="single.chatID === selectedChatId
                  ? 'border-amber-500/70 bg-amber-900/40 text-amber-50'
                  : 'border-amber-800/70 bg-stone-950/80 text-amber-100 hover:bg-stone-900/80'"
                  @click="selectedChatId = single.chatID"
              >
                <div class="flex items-center justify-between gap-2">
                  <span class="font-semibold">
                    {{ single.persona.name }}
                    <span class="text-amber-300/70 text-xs">↔</span>
                    {{ single.npc.name }}
                  </span>
                  <span class="text-[0.65rem] text-amber-300/80">
                    #{{ single.chatID }}
                  </span>
                </div>
                <p
                    v-if="single.messages.length > 0"
                    class="text-xs text-amber-200/80 line-clamp-2"
                >
                  “{{ single.messages[single.messages.length - 1].response }}”
                </p>
                <p
                    v-else
                    class="text-[0.65rem] text-amber-300/60"
                >
                  No messages yet.
                </p>
              </button>
            </li>
          </ul>
        </div>
      </section>

      <!-- RIGHT: chat viewer + send box -->
      <section
          class="rounded-2xl border border-amber-800/60 bg-stone-950/80 p-4 flex flex-col shadow-[0_0_30px_rgba(0,0,0,0.6)]"
      >
        <template v-if="selectedChat">
          <header class="mb-3 flex items-center justify-between border-b border-amber-900/70 pb-2">
            <div>
              <h2 class="text-lg font-semibold text-amber-300">
                {{ selectedChat.persona.name }}
                <span class="text-xs text-amber-300/70">↔</span>
                {{ selectedChat.npc.name }}
              </h2>
              <p class="text-[0.7rem] text-amber-200/70">
                Chat ID: #{{ selectedChat.chatID }}
              </p>
            </div>
            <div class="flex flex-col items-end gap-1 text-[0.7rem] text-amber-300/70">
              <span>
                Persona ID: <span class="font-semibold">{{ selectedChat.persona.id }}</span>
              </span>
              <span>
                NPC ID: <span class="font-semibold">{{ selectedChat.npc.id }}</span>
              </span>
            </div>
          </header>

          <!-- Messages area -->
          <div class="flex-1 overflow-y-auto pr-1 space-y-3 mb-3">
            <div
                v-if="selectedChat.messages.length === 0"
                class="text-xs text-amber-300/70"
            >
              No messages yet in this chat.
            </div>
            <div
                v-for="(msg, idx) in selectedChat.messages"
                :key="idx"
                class="flex flex-col gap-1"
            >
              <!-- Prompt (speaker) -->
              <div class="flex justify-start">
                <div class="max-w-[80%] rounded-lg bg-stone-900/90 border border-amber-900/60 px-3 py-2">
                  <div class="flex items-center justify-between gap-2 mb-0.5">
                    <span class="text-[0.7rem] font-semibold text-amber-200">
                      {{ msg.speaker.name }}
                    </span>
                    <span class="text-[0.6rem] text-amber-300/60">
                      to {{ msg.present.name }}
                    </span>
                  </div>
                  <p class="text-xs text-amber-100">
                    {{ msg.prompt }}
                  </p>
                </div>
              </div>

              <!-- Response (activeResponse) -->
              <div class="flex justify-end">
                <div class="max-w-[80%] rounded-lg bg-amber-700/90 border border-amber-400/70 px-3 py-2">
                  <div class="flex items-center justify-between gap-2 mb-0.5">
                    <span class="text-[0.7rem] font-semibold text-stone-950">
                      {{ msg.present.name }}
                    </span>
                    <span class="text-[0.6rem] text-stone-900/80">
                      reply
                    </span>
                  </div>
                  <p class="text-xs text-stone-950">
                    {{ msg.response }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- Send box -->
          <div class="border-t border-amber-900/70 pt-2">
            <label class="block text-xs font-semibold text-amber-200 mb-1">
              Send message as persona
            </label>
            <textarea
                v-model="userMessage"
                rows="3"
                class="w-full rounded-lg border border-amber-700/60 bg-stone-900/80 px-3 py-2 text-sm text-amber-100 placeholder:text-amber-400/50 focus:outline-none focus:ring-2 focus:ring-amber-400/70 focus:border-amber-300/80"
                placeholder="Speak to the NPC as if you were sitting at the tavern table..."
            ></textarea>
            <div class="mt-2 flex items-center justify-between gap-3">
              <label class="flex items-center gap-2 text-xs text-amber-300/80">
                <input
                    v-model="generateResponse"
                    type="checkbox"
                    class="h-3 w-3 rounded border-amber-500 bg-stone-900"
                />
                Ask the system to generate an NPC reply
              </label>
              <button
                  type="button"
                  class="rounded-md bg-amber-600 px-4 py-1.5 text-sm font-semibold text-stone-950 shadow-md shadow-amber-900/50 transition hover:bg-amber-500 hover:shadow-lg hover:shadow-amber-900/60 disabled:opacity-40 disabled:hover:shadow-none"
                  :disabled="sending || !userMessage.trim() || !selectedChatId"
                  @click="sendMessageToChat"
              >
                {{ sending ? 'Sending...' : 'Send message' }}
              </button>
            </div>
            <p v-if="sendInfo" class="mt-1 text-[0.7rem] text-emerald-300/80">
              {{ sendInfo }}
            </p>
          </div>
        </template>

        <template v-else>
          <div class="flex-1 flex items-center justify-center">
            <p class="text-sm text-amber-200/80 text-center max-w-xs">
              Select a tavern conversation on the left to view and send messages,
              or forge a new chat by pairing a persona ID with an NPC ID.
            </p>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

/* --- DTO types matching your Java records --- */

interface NPC_DTO {
  id: number
  name: string
  description: string
  physicalDescription: string
  miscDescription: string
}

interface Persona_DTO {
  id: number
  name: string
  description: string
}

interface Character_DTO {
  id: number
  name: string
  description: string
}

interface Single_ChatMessage_DTO {
  response: string
  prompt: string
  speaker: Character_DTO
  present: Character_DTO
}

interface Single_DTO {
  chatID: number
  persona: Persona_DTO
  npc: NPC_DTO
  messages: Single_ChatMessage_DTO[]
}

/* --- API base --- */

const CHAT_API = 'http://localhost:8080/api/chat'

/* --- State --- */

const singles = ref<Single_DTO[]>([])
const loadingSingles = ref(false)
const creating = ref(false)
const sending = ref(false)
const error = ref<string | null>(null)
const createMessage = ref<string | null>(null)
const sendInfo = ref<string | null>(null)

const personaIdInput = ref<number | null>(null)
const npcIdInput = ref<number | null>(null)
const selectedChatId = ref<number | null>(null)
const userMessage = ref('')
const generateResponse = ref(true)

/* --- Computed selected chat --- */

const selectedChat = computed<Single_DTO | null>(() => {
  if (selectedChatId.value == null) return null
  return singles.value.find(s => s.chatID === selectedChatId.value) ?? null
})

/* --- Load chats --- */

async function refreshSingles(): Promise<void> {
  loadingSingles.value = true
  error.value = null
  try {
    const response = await fetch(`${CHAT_API}/getSingles`)
    if (!response.ok) {
      throw new Error(`getSingles request failed: HTTP ${response.status}`)
    }
    const data = await response.json()
    singles.value = data
    // If current selection disappeared, clear it.
    if (
        selectedChatId.value != null &&
        !singles.value.some(s => s.chatID === selectedChatId.value)
    ) {
      selectedChatId.value = null
    }
  } catch (e: unknown) {
    const err = e as Error
    error.value = err.message ?? 'Failed to load chats'
  } finally {
    loadingSingles.value = false
  }
}

/* --- Create chat via IDs --- */

async function createSingleChat(): Promise<void> {
  if (!personaIdInput.value || !npcIdInput.value) return

  creating.value = true
  error.value = null
  createMessage.value = null

  try {
    const params = new URLSearchParams({
      userId: String(personaIdInput.value),
      NPC_ID: String(npcIdInput.value),
    })
    const response = await fetch(`${CHAT_API}/createSingle?${params.toString()}`)
    if (!response.ok) {
      throw new Error(`createSingle failed: HTTP ${response.status}`)
    }
    const text = await response.text()
    createMessage.value = text || 'Chat created successfully.'
    await refreshSingles()
  } catch (e: unknown) {
    const err = e as Error
    error.value = err.message ?? 'Failed to create chat'
  } finally {
    creating.value = false
  }
}

/* --- Send message --- */

async function sendMessageToChat(): Promise<void> {
  if (!selectedChatId.value || !userMessage.value.trim()) return

  sending.value = true
  error.value = null
  sendInfo.value = null

  try {
    // 1) call /sendMessage
    const params = new URLSearchParams({
      chatID: String(selectedChatId.value),
      message: userMessage.value,
      generateResponse: String(generateResponse.value),
    })
    const response = await fetch(`${CHAT_API}/sendMessage?${params.toString()}`)
    if (!response.ok) {
      throw new Error(`sendMessage failed: HTTP ${response.status}`)
    }
    const text = await response.text()
    sendInfo.value = text || 'Message sent.'

    // 2) fetch last message and append it to the local chat
    const lmParams = new URLSearchParams({ chatID: String(selectedChatId.value) })
    const lastResp = await fetch(`${CHAT_API}/single/lastMessage?${lmParams.toString()}`)
    if (!lastResp.ok) {
      throw new Error(`lastMessage request failed: HTTP ${lastResp.status}`)
    }
    const lastMessage: Single_ChatMessage_DTO | null = await lastResp.json()

    if (lastMessage) {
      const chat = singles.value.find(s => s.chatID === selectedChatId.value)
      if (chat) {
        chat.messages.push(lastMessage)
      }
    }

    // clear input
    userMessage.value = ''
  } catch (e: unknown) {
    const err = e as Error
    error.value = err.message ?? 'Failed to send message'
  } finally {
    sending.value = false
  }
}

/* --- Initial load --- */

onMounted(async () => {
  try {
    await refreshSingles()
  } catch (e: unknown) {
    const err = e as Error
    error.value = err.message ?? 'Failed to initialize chat box'
  }
})
</script>
