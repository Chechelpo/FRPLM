import {ABSEntity, createEntity, deleteEntity, fetchApi, fetchMatching, fetchOne} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {Location, LocationData, LocationKey, World, WorldData, WorldKey} from "@/domain/World";
import {Character, CharacterData, CharacterKey} from "@/domain/Characters";
import {DTO} from "@/types/DTOs";
import {PromptTemplate, PromptTemplateData, PromptTemplateKey} from "@/domain/Prompts";
import {ChatCompletionRequest, ChatCompletionRole} from "@/types/ChatCompletions";
import {API_BASE} from "@/config";

export type SessionKey = {id:number};
export type SessionData = {
    name:string,
    template_id:number | null,
    current_tick:number,
    world_id:number,
    user_id:number
};
export class Session extends ABSEntity<SessionKey,SessionData>{
    getEntityType(): EntityTypes {
        return EntityTypes.SESSIONS;
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
    public async getNewPrompt(order:NewMessageOrder) : Promise<ChatCompletionRequest> {
        if (!order.debugPrompt) throw new Error("Called for a new prompt without debug prompt being implemented")
        console.debug(`Asking for a new prompt for session ${this}`)

        return await fetchApi(
            `${API_BASE}/engine/prompt/${this.get('id')}`,
            {
                method:'GET'
            }
        ).then(async response => await response.json() as ChatCompletionRequest)
    }
    public async generateNewMessage() : Promise<Message> {
        return await fetchApi(
            `${API_BASE}/engine/${this.get('id')}`,
            {
                method:'POST',
                headers: {
                    'Content-Type': "application/json",
                },
                body: JSON.stringify({
                    streaming:false,
                    autoResponse:true,
                    debugPrompt:false
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
    message: string;
    debugPrompt: boolean;
}

export type MessagesKey = {session_id:number, tick_num:number};
export type MessageData = {
    world_id:number,
    location_id:number,
    time: number,
    prompt?: string,
    role:ChatCompletionRole,
    content:string
}
export class Message extends ABSEntity<MessagesKey, MessageData>{
    override getEntityType(): EntityTypes {
        return EntityTypes.MESSAGES;
    }

    async regenerate() : Promise<void> {
        if (this.get('role') != ChatCompletionRole.ASSISTANT) return;
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
            `${API_BASE}/${EntityTypes.CURRENT_LOCATION}/${this.get('session_id')}/${this.get('location_id')}`,
            {
                method:'GET'
            }
        ).then(async response => (await response.json() as DTO[]).map(dto => new Character(dto, EntityTypes.CHARACTERS)))
    }
}