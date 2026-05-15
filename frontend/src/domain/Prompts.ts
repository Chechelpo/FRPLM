import {ABSEntity, API_BASE, createEntity, fetchApi} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";


export type REASONING_EFFORT_VALUE = {id:number, name:string}
export const REASONING_EFFORT = {
    NONE    :{id:0, name: "none"},
    MINIMAL :{id:1, name: "minimal"},
    LOW     :{id:2, name: "low"},
    MEDIUM  :{id:3, name: "medium"},
    HIGH    :{id:4, name: "high"},
    MAXIMUM :{id:5, name: "xhigh"},
} as const satisfies Record<string, REASONING_EFFORT_VALUE>
export type ReasoningEffort = typeof REASONING_EFFORT[keyof typeof REASONING_EFFORT];
export type ReasoningEffortId = ReasoningEffort["id"];

export const REASONING_EFFORT_IDs = Object.values(REASONING_EFFORT).map(value => value.id);
export const REASONING_EFFORT_NAMEs = Object.values(REASONING_EFFORT).map(value => value.name);

export type PromptTemplateKey = {id:number}
export type PromptTemplateData = {
    connection_id:number | null,
    name: string,
    max_tokens: number,
    streaming: boolean,

    temperature: number,
    top_p: number,
    frequency_penalty: number,
    presence_penalty: number,
    repetition_penalty: number,
    top_k: number,

    exclude_reasoning: boolean,
    reasoning_effort: ReasoningEffortId,
}

export class PromptTemplate extends ABSEntity<PromptTemplateKey, PromptTemplateData>{
    private sections: PromptSection[] | null = null;

    getEntityType(): EntityTypes {
        return EntityTypes.TEMPLATES;
    }
    public async getSections(): Promise<PromptSection[]> {
        if (this.sections == null)
            this.sections = await getPromptSections(this.key)

        return this.sections;
    }

    public async createSection(name:string): Promise<PromptSection> {
        const newPromptSection = await createEntity<PromptSectionKey, PromptSectionData, PromptSection>(
            {
                prompt_id: this.get('id'),
            },
            {
                name:name
            },
            EntityTypes.SECTIONS,
            PromptSection
        )

        await this.getSections()
        this.sections!.push(newPromptSection);

        return newPromptSection;
    }
}

export enum Role {
    USER = 'user',
    ASSISTANT = 'assistant',
    SYSTEM = 'system',
}
export type PromptSectionKey = {prompt_id:number, section_id:number};
export type PromptSectionData = {name:string, active:boolean, position:number, role:Role, content:string};
export class PromptSection extends ABSEntity<PromptSectionKey, PromptSectionData>{
    getEntityType(): EntityTypes {
        return EntityTypes.SECTIONS;
    }
}

async function getPromptSections(ofKey:PromptTemplateKey) : Promise<PromptSection[]>{
    return await fetchApi(
        `${API_BASE}/${EntityTypes.SECTIONS}/ofTemplate/${ofKey.id}`,
        {
            method: "GET",
            headers: {
                Accept: "application/json",
            }
        }
    )
    .then(
        async response =>
            (await response.json() as DTO[])
                .map(dto => new PromptSection(dto))
    )
}