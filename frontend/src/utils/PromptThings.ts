import {Entry, Lorebook} from "@/domain/Lorebook";
import {ChatCompletionRequest} from "@/types/ChatCompletions";
import {PromptDTO} from "@/types/DTOs";
import {EntityTypes} from "@/domain/EntityTypes";

export interface DomainPromptDTO {
    lorebooks: Lorebook[];
    entriesByLorebookId: Map<number, Entry[]>;
    rawRequest:ChatCompletionRequest;
}

export function toDomainPrompt(promptDto:PromptDTO): DomainPromptDTO {
    const entriesById = new Map<number, Entry[]>();
    promptDto.activatedEntries.forEach(entryDto => {
        const entry = new Entry(entryDto, EntityTypes.ENTRY);
        const lorebookId = entry.get('lorebook_id');

        if (!entriesById.has(lorebookId)) entriesById.set(lorebookId, [])
        entriesById.get(lorebookId)!.push(entry);
    })

    return {
        lorebooks: promptDto.lorebooks.map(dto => new Lorebook(dto, EntityTypes.LOREBOOKS)),
        entriesByLorebookId:entriesById,
        rawRequest: promptDto.rawRequest
    }
}