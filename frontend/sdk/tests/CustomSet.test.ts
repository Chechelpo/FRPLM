// tests/CustomSet.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";

import { CustomSet } from "../dist/utils/CustomSet.js";
import type { ValueComparable } from "../dist/types/Equatable.js";

class Box implements ValueComparable {
    public readonly id: number;
    public label: string;

    constructor(id: number, label: string) {
        this.id = id;
        this.label = label;
    }

    public hashKey(): string {
        return `box:${this.id}`;
    }

    public equals(other: unknown): boolean {
        return other instanceof Box
            && this.id === other.id
            && this.label === other.label;
    }
}

test("CustomSet adds unique values and ignores duplicates", () => {
    const set = new CustomSet<Box>();
    const a = new Box(1, "a");
    const aClone = new Box(1, "a");

    set.add(a);
    set.add(aClone);
    set.add(new Box(2, "b"));

    assert.equal(set.size, 2);
    assert.deepEqual(set.values().map(v => v.id).sort(), [1, 2]);
});

test("CustomSet.has is identity-aware", () => {
    const set = new CustomSet<Box>();
    const a = new Box(1, "a");
    set.add(a);

    assert.equal(set.has(a), true);
    assert.equal(set.has(new Box(1, "a")), true);
    assert.equal(set.has(new Box(1, "different")), false);
    assert.equal(set.has(new Box(2, "a")), false);
});

test("CustomSet.delete only removes identity-matching values", () => {
    const set = new CustomSet<Box>();
    set.add(new Box(1, "a"));

    assert.equal(set.delete(new Box(1, "a")), true);
    assert.equal(set.delete(new Box(1, "a")), false);
    assert.equal(set.size, 0);
});

test("CustomSet.add_all adds every element in the iterable", () => {
    const set = new CustomSet<Box>();
    set.add_all([new Box(1, "a"), new Box(2, "b"), new Box(1, "a")]);

    assert.equal(set.size, 2);
});
