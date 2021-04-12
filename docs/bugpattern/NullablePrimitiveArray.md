For `@Nullable` type annotations (such as
`org.checkerframework.checker.nullness.qual.Nullable`), `@Nullable byte[]` means
a 'non-null array of nullable bytes', and `byte @Nullable []` means a 'nullable
array of non-null bytes'. Since primitive types cannot be null, the former is
incorrect.

Some other nullness annotations (such as `javax.annotation.Nullable`) are
_declaration_ annotations rather than _type_ annotations. Their meaning is
different: For such annotations, `@Nullable byte[]` refers to 'a nullable array
of non-null bytes,' and `byte @Nullable []` is rejected by javac. Thus, this
check never reports errors for usages of declaration annotations.

See also: https://checkerframework.org/manual/#faq-array-syntax-meaning
