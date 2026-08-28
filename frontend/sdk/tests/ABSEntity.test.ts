// tests/ABSEntity.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";

import { ABSEntity } from "../dist/core/ABSEntity.js";
import { EntityTypes } from "../dist/domain/EntityTypes.js";
import { CommonFields } from "../dist/utils/CommonFields.js";
import type { DTO } from "../dist/types/DTOs.js";

class Tag extends ABSEntity<{ id: number }, { name: string }> {
    private static readonly KEY_ORDER = ["id"] as const;

    getEntityType(): EntityTypes {
        return EntityTypes.TAGS;
    }

    protected getReferenceKeyOrder(): readonly ["id"] {
        return Tag.KEY_ORDER;
    }
}

function makeTagDTO(
    id: number,
    name: string,
): DTO {
    return {
        type: EntityTypes.TAGS,
        key: { id },
        payload: { name },
    };
}

test("ABSEntity stores key and data, exposes them through get()", () => {
    const tag = new Tag(makeTagDTO(1, "alpha"), EntityTypes.TAGS);

    assert.deepEqual(tag.key, { id: 1 });
    assert.equal(tag.get("id"), 1);
    assert.equal(tag.get("name"), "alpha");
});

test("ABSEntity.get throws when the requested field is unknown", () => {
    const tag = new Tag(makeTagDTO(1, "alpha"), EntityTypes.TAGS);

    assert.throws(() => tag.get("missing" as never));
});

test("ABSEntity.hasAttribute distinguishes key, data, and unknown", () => {
    const tag = new Tag(makeTagDTO(1, "alpha"), EntityTypes.TAGS);

    assert.equal(tag.hasAttribute("id"), true);
    assert.equal(tag.hasAttribute("name"), true);
    assert.equal(tag.hasAttribute("missing"), false);
});

test("ABSEntity.getCommon looks up fields by CommonFields enum", () => {
    const tag = new Tag(makeTagDTO(1, "alpha"), EntityTypes.TAGS);

    assert.equal(tag.getCommon(CommonFields.NAME), "alpha");
    assert.equal(tag.getCommon(CommonFields.DESCRIPTION), null);
});

test("ABSEntity.equals is identity-aware by composite key", () => {
    const a = new Tag(makeTagDTO(1, "alpha"), EntityTypes.TAGS);
    const aClone = new Tag(makeTagDTO(1, "alpha"), EntityTypes.TAGS);
    const different = new Tag(makeTagDTO(2, "beta"), EntityTypes.TAGS);

    assert.equal(a.equals(a), true);
    assert.equal(a.equals(aClone), true);
    assert.equal(a.equals(different), false);
    assert.equal(a.equals(null), false);
    assert.equal(a.equals("not a tag"), false);
});

test("ABSEntity.hashKey is stable for the same key, distinct for different keys", () => {
    const a = new Tag(makeTagDTO(1, "alpha"), EntityTypes.TAGS);
    const b = new Tag(makeTagDTO(1, "alpha"), EntityTypes.TAGS);
    const c = new Tag(makeTagDTO(2, "beta"), EntityTypes.TAGS);

    assert.equal(a.hashKey(), b.hashKey());
    assert.notEqual(a.hashKey(), c.hashKey());
});

test("ABSEntity rejects DTOs that mix key and data fields", () => {
    const conflicting: DTO = {
        type: EntityTypes.TAGS,
        key: { id: 1 },
        payload: { id: 1, name: "alpha" },
    };

    assert.throws(() => new Tag(conflicting, EntityTypes.TAGS));
});

test("ABSEntity rejects DTOs of the wrong entity type", () => {
    const wrongType: DTO = {
        type: EntityTypes.CHARACTERS,
        key: { id: 1 },
        payload: { name: "alpha" },
    };

    assert.throws(() => new Tag(wrongType, EntityTypes.TAGS));
});

test("ABSEntity rejects DTOs with empty key maps", () => {
    const empty: DTO = {
        type: EntityTypes.TAGS,
        key: {},
        payload: { name: "alpha" },
    };

    assert.throws(() => new Tag(empty, EntityTypes.TAGS));
});
