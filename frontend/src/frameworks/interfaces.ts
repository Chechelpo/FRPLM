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