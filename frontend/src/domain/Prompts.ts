import {ABSEntity, createEntity, fetch_all, fetchApi, fetchMatching} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {ChatCompletionRole} from "@/types/ChatCompletions";
import {API_BASE} from "@/config";


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
        return await fetchMatching<PromptSectionKey, PromptSectionData, PromptSection>(
            {
                prompt_id: this.get('id')
            },
            EntityTypes.SECTIONS,
            PromptSection
        );
    }

    public async createSection(name:string): Promise<PromptSection> {
        return await createEntity<PromptSectionKey, PromptSectionData, PromptSection>(
            {
                prompt_id: this.get('id'),
            },
            {
                name: name
            },
            EntityTypes.SECTIONS,
            PromptSection
        );
    }

    public static async getAll(): Promise<PromptTemplate[]> {
        return await fetch_all<PromptTemplateKey, PromptTemplateData, PromptTemplate>(
            EntityTypes.TEMPLATES, PromptTemplate
        )
    }
}


export type PromptSectionKey = {prompt_id:number, section_id:number};
export type PromptSectionData = {name:string, active:boolean, position:number, role:ChatCompletionRole, content:string};
export class PromptSection extends ABSEntity<PromptSectionKey, PromptSectionData>{
    getEntityType(): EntityTypes {
        return EntityTypes.SECTIONS;
    }

    public static async exchange(parent:PromptTemplate, one:PromptSection, two:PromptSection): Promise<boolean> {
        const response = await fetchApi(
            `${API_BASE}/${EntityTypes.SECTIONS}/exchange/${parent.get('id')}/${one.get('section_id')}/${two.get('section_id')}`,
            {
                method:'POST'
            }
        )
        if (response.status === 200){
            let temp = one.get('position');
            one.dataMap.position = two.get('position');
            two.dataMap.position = temp;
        }
        return response.status === 200;
    }
}