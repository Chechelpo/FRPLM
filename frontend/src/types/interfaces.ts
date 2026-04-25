/** @deprecated */
export interface BackendObject{
    id: number;
    description: string;
    url: string;
}
/** @deprecated*/
export interface NamedObject extends BackendObject {
    name: string;
}
/** @deprecated */
export interface LLM_connection{
    id: number;
    name: string;
    url: string;
    type: string;
}