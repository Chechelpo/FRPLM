package io.github.chechelpo.frplm.core.entities.fields.kinds;

public sealed interface FieldKind {
    final class StringKind implements FieldKind{}
    final class NumberKind implements FieldKind{}
    final class FloatKind implements FieldKind{}
    final class BooleanKind implements FieldKind{}
    final class EnumKind implements FieldKind{}
}