// tests/ReferenceCodec.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";

import {
    ReferenceCodec,
    parseNumberKey,
    parseStringKey,
    parseBooleanKey,
} from "../dist/utils/ReferenceCodec.js";
import { EntityTypes } from "../dist/domain/EntityTypes.js";

test("encodeKey then decodeKey round-trips", () => {
    const encoded = ReferenceCodec.encodeKey(
        EntityTypes.WORLDS,
        { id: 42 },
        ["id"],
    );

    assert.equal(encoded, "worlds42");

    const decoded = ReferenceCodec.decodeKey(
        encoded,
        EntityTypes.WORLDS,
        ["id"],
        { id: parseNumberKey },
    );

    assert.deepEqual(decoded, { id: 42 });
});

test("encodeKey throws on null/undefined values", () => {
    assert.throws(() =>
        ReferenceCodec.encodeKey(
            EntityTypes.WORLDS,
            { id: null as unknown as number },
            ["id"],
        ),
    );

    assert.throws(() =>
        ReferenceCodec.encodeKey(
            EntityTypes.WORLDS,
            {} as { id: number },
            ["id"],
        ),
    );
});

test("decodeKey rejects references with the wrong entity prefix", () => {
    assert.throws(() =>
        ReferenceCodec.decodeKey(
            "locations1",
            EntityTypes.WORLDS,
            ["id"],
            { id: parseNumberKey },
        ),
    );
});

test("decodeKey rejects references with the wrong number of parts", () => {
    assert.throws(() =>
        ReferenceCodec.decodeKey(
            "worlds1,2",
            EntityTypes.WORLDS,
            ["id"],
            { id: parseNumberKey },
        ),
    );
});

test("decodeKey rejects references with empty parts", () => {
    assert.throws(() =>
        ReferenceCodec.decodeKey(
            "worlds,",
            EntityTypes.WORLDS,
            ["id"],
            { id: parseNumberKey },
        ),
    );
});

test("decodeKey rejects references with surrounding whitespace", () => {
    assert.throws(() =>
        ReferenceCodec.decodeKey(
            "worlds 1",
            EntityTypes.WORLDS,
            ["id"],
            { id: parseNumberKey },
        ),
    );
});

test("parseNumberKey parses integers and rejects non-integers", () => {
    assert.equal(parseNumberKey("0"), 0);
    assert.equal(parseNumberKey("-7"), -7);
    assert.equal(parseNumberKey("42"), 42);

    assert.throws(() => parseNumberKey("3.14"));
    assert.throws(() => parseNumberKey("abc"));
    assert.throws(() => parseNumberKey(""));
    assert.throws(() => parseNumberKey("1a"));
});

test("parseBooleanKey parses true/false and rejects other strings", () => {
    assert.equal(parseBooleanKey("true"), true);
    assert.equal(parseBooleanKey("false"), false);

    assert.throws(() => parseBooleanKey("yes"));
    assert.throws(() => parseBooleanKey(""));
});

test("parseStringKey is identity", () => {
    assert.equal(parseStringKey("hello"), "hello");
    assert.equal(parseStringKey(""), "");
});
