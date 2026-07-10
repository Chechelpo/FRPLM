// ReferenceCodec.ts

import { EntityTypes } from "@/domain/EntityTypes";
import { KeyRecord, Primitives } from "@/core/ABSEntity";

export type ReferenceKeyValue = Exclude<Primitives, null>;

export type ReferenceParsers<Key extends KeyRecord> = {
    [K in keyof Key]: (raw: string) => Key[K];
};

export class ReferenceCodec {
    private static readonly SEPARATOR = ",";

    private constructor() {}

    public static encode(
        entityType: EntityTypes,
        values: readonly ReferenceKeyValue[],
    ): string {
        if (values.length === 0) {
            throw new Error("Reference must contain at least one key value");
        }

        return `${entityType}${values.join(ReferenceCodec.SEPARATOR)}`;
    }

    public static encodeKey<Key extends KeyRecord>(
        entityType: EntityTypes,
        key: Readonly<Key>,
        keyOrder: readonly (keyof Key & string)[],
    ): string {
        const values = keyOrder.map((field) => {
            const value = key[field];

            if (value === null) {
                throw new Error(
                    `Cannot encode null reference key field '${field}' for ${entityType}`,
                );
            }

            if (value === undefined) {
                throw new Error(
                    `Cannot encode undefined reference key field '${field}' for ${entityType}`,
                );
            }

            return value;
        });

        return ReferenceCodec.encode(entityType, values);
    }

    public static decodeKey<Key extends KeyRecord>(
        reference: string,
        entityType: EntityTypes,
        keyOrder: readonly (keyof Key & string)[],
        parsers: ReferenceParsers<Key>,
    ): Key {
        if (!reference.startsWith(entityType)) {
            throw new Error(
                `Reference does not start with expected entity type '${entityType}': ${reference}`,
            );
        }

        const raw = reference.substring(entityType.length);

        if (raw.length === 0) {
            throw new Error(
                `Reference is missing key payload after entity type '${entityType}': ${reference}`,
            );
        }

        const parts = raw.split(ReferenceCodec.SEPARATOR);

        if (parts.length !== keyOrder.length) {
            throw new Error(
                `Expected ${keyOrder.length} key part(s) for ${entityType}, ` +
                `but found ${parts.length}: ${reference}`,
            );
        }

        const key = {} as Key;

        for (let i = 0; i < keyOrder.length; i++) {
            const field = keyOrder[i];
            const rawValue = parts[i];

            if (rawValue.length === 0) {
                throw new Error(
                    `Reference key field '${field}' is empty: ${reference}`,
                );
            }

            if (rawValue !== rawValue.trim()) {
                throw new Error(
                    `Reference key field '${field}' contains surrounding whitespace: ${reference}`,
                );
            }

            key[field] = parsers[field](rawValue);
        }

        return key;
    }
}

export function parseNumberKey(raw: string): number {
    if (!/^-?\d+$/.test(raw)) {
        throw new Error(`Expected integer reference key, got: ${raw}`);
    }

    const value = Number(raw);

    if (!Number.isSafeInteger(value)) {
        throw new Error(`Reference key is not a safe JavaScript integer: ${raw}`);
    }

    return value;
}

export function parseStringKey(raw: string): string {
    return raw;
}

export function parseBooleanKey(raw: string): boolean {
    if (raw === "true") return true;
    if (raw === "false") return false;

    throw new Error(`Expected boolean reference key, got: ${raw}`);
}