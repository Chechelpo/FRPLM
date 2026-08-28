// tests/queries.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";

import {
    QueryActionType,
    type QueryAction,
} from "../dist/core/queries.js";

test("QueryActionType exposes the expected wire values", () => {
    assert.equal(QueryActionType.EQUALS, "EQUALS");
    assert.equal(QueryActionType.LIKE, "LIKE");
    assert.equal(QueryActionType.GREATER_THAN, "GREATER_THAN");
    assert.equal(QueryActionType.LESS_THAN, "LESS_THAN");
});

test("QueryAction values flow through the generic type", () => {
    const query: QueryAction<
        { id: number },
        { name: string; age: number }
    > = {
        fieldName: "name",
        action: QueryActionType.LIKE,
        value: "alice",
    };

    assert.equal(query.fieldName, "name");
    assert.equal(query.action, "LIKE");
    assert.equal(query.value, "alice");
});
