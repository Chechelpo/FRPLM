export interface Equatable{
    equals(other:unknown): boolean
}
export interface Hashable{
    hashKey(): string;
}
export interface ValueComparable extends Hashable, Equatable{}