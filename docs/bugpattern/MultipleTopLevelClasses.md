The Google Java Style Guide §3.4.1 requires each source file to contain exactly
one top-level class.

## Suppression

Since `@SuppressWarnings` cannot be applied to package declarations, this
warning can be suppressed by annotating any top-level class in the compilation
unit with `@SuppressWarnings("MultipleTopLevelClasses")`.
