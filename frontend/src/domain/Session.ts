import {
    ABSEntity,
    createEntity,
    deleteEntity,
    fetchFromReference,
    fetchMatching,
    getEntityController,
    fetchOne
} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {Location, LocationData, LocationKey, World, WorldData, WorldKey} from "@/domain/World";
import {Character, CharacterData, CharacterKey} from "@/domain/Characters";
import {DTO, PromptDTO} from "@/types/DTOs";
import {PromptTemplate, PromptTemplateData, PromptTemplateKey} from "@/domain/Prompts";
import {ChatCompletionRequest, ChatCompletionRole} from "@/types/ChatCompletions";
import {API_BASE} from "@/config";
import {parseNumberKey} from "@/utils/ReferenceCodec";
import {fetchApi} from "@/services/apiClient";

export type SessionKey = {id:number};
export type SessionData = {
    name:string,
    template_id:number | null,
    current_tick:number,
    world_id:number,
    user_id:number
};
export class Session extends ABSEntity<SessionKey,SessionData>{
    private static readonly REFERENCE_KEY_ORDER: readonly (keyof SessionKey & string)[] = ['id'] as const;

    getEntityType(): EntityTypes {
        return EntityTypes.SESSIONS;
    }

    protected getReferenceKeyOrder(): readonly (keyof SessionKey & string)[] {
        return Session.REFERENCE_KEY_ORDER;
    }

    public static async getFromReference(reference:string) : Promise<Session> {
        return await fetchFromReference<SessionKey, SessionData, Session>(
            reference, EntityTypes.SESSIONS, this.REFERENCE_KEY_ORDER, {id:parseNumberKey}, Session
        )
    }

    public async getWorld() : Promise<World> {
        return await fetchOne<WorldKey, WorldData, World>(
            {
                id:this.get('world_id')
            },
            EntityTypes.WORLDS,
            World
        )
    }
    public async getUserCharacter() : Promise<Character> {
        return await fetchOne<CharacterKey, CharacterData, Character>({id:this.get('user_id')}, EntityTypes.CHARACTERS, Character)
    }
    public async getMessages() : Promise<Message[]>{
        return fetchMatching<MessagesKey, MessageData, Message>(
            {
                session_id: this.get('id')
            },
            EntityTypes.MESSAGES,
            Message
        );
    }
    public async getTemplate() : Promise<PromptTemplate | null>{
        if (this.get('template_id') == null) return null;
        return await fetchOne<PromptTemplateKey, PromptTemplateData, PromptTemplate>(
            {
                id:this.get('template_id')!
            },
            EntityTypes.TEMPLATES,
            PromptTemplate
        );
    }
    public isLastMessage(message:Message) : boolean {
        if (message.get('session_id') != this.get('id'))
            throw new Error("This message is not from this session")
        return message.get('tick_num') == this.get('current_tick');
    }
    private incrementTick(): void{
        this.dataMap.current_tick = this.dataMap.current_tick + 1;
    }

    public async newUserMessage(lastMessage: Message, message:string): Promise<Message>{
        const newMessage = await createEntity<MessagesKey, MessageData, Message>(
            {
                session_id: this.get('id'),
            },
            {
                world_id: this.get('world_id'),
                location_id: lastMessage.get("location_id"),
                role: ChatCompletionRole.USER,
                content: message
            },
            EntityTypes.MESSAGES, Message
        )
        this.incrementTick()

        return newMessage
    }
    public async deleteMessage(message:Message) : Promise<boolean> {
        if (await deleteEntity<MessagesKey>(message.key, EntityTypes.MESSAGES)) {
            this.dataMap.current_tick = this.dataMap.current_tick - 1;
            return true;
        }
        return false;
    }
    public async getNewPrompt() : Promise<PromptDTO> {
        console.debug(`Asking for a new prompt for session ${this}`)

        return await fetchApi(
            `api/prompts/new/${this.get('id')}`,
            {
                method:'GET'
            }
        ).then(async response => await response.json() as PromptDTO)
    }
    public async generateNewMessage(request:ChatCompletionRequest) : Promise<Message> {
        if (!request) throw new Error("Request is null for new message");
        return await fetchApi(
            `api/engine/generate/${this.get('id')}`,
            {
                method:'POST',
                headers: {
                    'Content-Type': "application/json",
                },
                body: JSON.stringify({
                    streaming:false,
                    prompt:request
                })
            }
        ).then(async response => {
            this.incrementTick()
            return new Message(await response.json() as DTO, EntityTypes.MESSAGES)
        }
        )
    }

    public static async newSession(name:string, world:World, user:Character): Promise<Session>{
        return await createEntity<SessionKey,SessionData,Session>(
            null,
            {
                name:name,
                world_id:world.get('id'),
                user_id:user.get('id'),
            },
            EntityTypes.SESSIONS,
            Session
        )
    }
}

export interface NewMessageOrder {
    prompt: string;
}

export type MessagesKey = {session_id:number, tick_num:number};
export type MessageData = {
    world_id:number,
    location_id:number,
    time: number,
    prompt?: string,
    reasoning : string | null,
    role:ChatCompletionRole,
    content:string,
    response_num:number,
    active_response:number,
}
export class Message extends ABSEntity<MessagesKey, MessageData>{
    private static readonly REFERENCE_KEY_ORDER: readonly (keyof MessagesKey & string)[] = ['session_id', 'tick_num'] as const;

    override getEntityType(): EntityTypes {
        return EntityTypes.MESSAGES;
    }

    protected getReferenceKeyOrder(): readonly (keyof MessagesKey & string)[] {
        return Message.REFERENCE_KEY_ORDER;
    }

    async update<F extends keyof MessageData>(field: F, value: MessageData[F]): Promise<boolean> {
        if (field == 'active_response'){
            await super.update(field, value);
            const newMessage = await fetchOne<MessagesKey, MessageData, Message>(this.key, EntityTypes.MESSAGES, Message)
            this.dataMap = newMessage.dataMap
            return true;
        }
        return super.update(field, value);
    }

    public async regenerate():Promise<void> {
        if (this.get('role') != ChatCompletionRole.ASSISTANT) return;
        try{
            const newMessage = await fetchApi(
                `api/engine/regenerate?sessionID=${this.get('session_id')}&tick_num=${this.get("tick_num")}`,
                {
                    method: "POST",
                }
            ).then(async response => new Message(await response.json() as DTO, EntityTypes.MESSAGES))

            if (newMessage.get('session_id') != this.get('session_id')) {
                console.error("Regenerated a message of another session");
                return;
            }
            if (newMessage.get('tick_num') != this.get('tick_num')){
                console.error("Regenerated a message with another tick num");
                return;
            }

            this.dataMap = newMessage.dataMap;
        } catch (error) {
            console.error(`Error regenerating response: \n ${error}`);
        }
    }

    public async getLocation() : Promise<Location> {
        return await fetchOne<LocationKey, LocationData, Location>(
            {
                worldID: this.get('world_id'),
                id: this.get('location_id')
            },
            EntityTypes.LOCATIONS,
            Location
        );
    }

    public async getCharacters() : Promise<Character[]> {
        return await fetchApi(
            `${getEntityController(EntityTypes.CURRENT_LOCATION)}/${this.get('session_id')}/${this.get('location_id')}`,
            {
                method:'GET'
            }
        ).then(async response => (await response.json() as DTO[]).map(dto => new Character(dto, EntityTypes.CHARACTERS)))
    }


}