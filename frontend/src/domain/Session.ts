import {ABSEntity, createEntity} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {World} from "@/domain/World";
import {Character} from "@/domain/Characters";

export type SessionKey = {id:number};
export type SessionData = {name:string, world_id:number, user_id:number};
export class Session extends ABSEntity<SessionKey,SessionData>{
    getEntityType(): EntityTypes {
        return EntityTypes.SESSIONS;
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

export type MessagesKey = {session_id:number, tick_num:number};
export type MessageData = {
    location_id:number,
    time: number,
    user_generated:boolean,
    content:string
}
export class Message extends ABSEntity<MessagesKey, MessageData>{
    override getEntityType(): EntityTypes {
        return EntityTypes.MESSAGES;
    }

    public async regenerate() : Promise<void> {
        if (this.get('user_generated')) return;
    }
}