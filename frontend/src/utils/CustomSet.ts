import {ValueComparable} from "@/types/Equatable";

export class CustomSet<T extends ValueComparable> {
    private readonly map = new Map<string, T>();

    public constructor() {}

    public add(value: T): void {
        const key = value.hashKey();
        const existing = this.map.get(key);

        if (existing === undefined || !existing.equals(value)) {
            this.map.set(key, value);
        }
    }
    public add_all(values:T[]):void{
        for (const value of values){
            this.add(value)
        }
    }

    public has(value: T): boolean {
        const existing = this.map.get(value.hashKey());
        return existing !== undefined && existing.equals(value);
    }

    public delete(value: T): boolean {
        const existing = this.map.get(value.hashKey());
        if (existing !== undefined && existing.equals(value)) {
            return this.map.delete(value.hashKey());
        }
        return false;
    }

    public values(): T[] {
        return [...this.map.values()];
    }

    public get size(): number {
        return this.map.size;
    }
}